import { MarkerType } from "reactflow";
import getLayoutedElements from "./layout";

const NODE_STYLES = {
  PACKAGE: { background: "#2563eb", borderRadius: "16px" },
  PROCEDURE: { background: "#7c3aed", borderRadius: "999px" },
  FUNCTION: { background: "#9333ea", borderRadius: "999px" },
  TRIGGER: { background: "#f97316", borderRadius: "10px" },
  VIEW: { background: "#a855f7", borderRadius: "8px" },
  TABLE: { background: "#16a34a", borderRadius: "4px" },
  UNKNOWN: { background: "#64748b", borderRadius: "10px" }
};

const RELATION_COLORS = {
  CONTAINS: "#818cf8",
  READS: "#38bdf8",
  WRITES: "#f97316",
  CALLS: "#c084fc"
};

function uniqueRelations(relations) {
  const seen = new Set();
  return relations.filter(({ source, target, relation }) => {
    const key = `${source}\u001f${relation}\u001f${target}`;
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });
}

function inferType(id, relation, endpoint) {
  const value = id.toUpperCase();
  if (value.startsWith("PKG_")) return "PACKAGE";
  if (value.startsWith("SP_") || value.includes(".SP_")) return "PROCEDURE";
  if (value.startsWith("FN_") || value.includes(".FN_")) return "FUNCTION";
  if (value.startsWith("TRG_")) return "TRIGGER";
  if (value.startsWith("VW_") || value.startsWith("V_")) return "VIEW";
  if (endpoint === "target" && ["READS", "WRITES"].includes(relation)) return "TABLE";
  return "UNKNOWN";
}

export function buildKnowledgeGraph(payload = {}) {
  const responseRelations = Array.isArray(payload.knowledgeRelations)
    ? payload.knowledgeRelations
    : Array.isArray(payload.edges) ? payload.edges : [];
  const declaredNodes = Array.isArray(payload.nodes) ? payload.nodes : [];
  const subprograms = Array.isArray(payload.subprograms) ? payload.subprograms : [];
  const nodeTypes = new Map();

  const packageName = payload.name;
  if (packageName) {
    const payloadType = (payload.type || "PACKAGE").toUpperCase();
    nodeTypes.set(packageName, payloadType.includes("PACKAGE") ? "PACKAGE" : payloadType);
  }

  subprograms.forEach((subprogram) => {
    const id = subprogram.qualifiedName || (packageName && `${packageName}.${subprogram.name}`) || subprogram.name;
    if (id) nodeTypes.set(id, (subprogram.type || "PROCEDURE").toUpperCase());
  });

  declaredNodes.forEach((node) => {
    const id = typeof node === "string" ? node : node?.id;
    if (id) nodeTypes.set(id, typeof node === "string" ? "UNKNOWN" : node.type || "UNKNOWN");
  });

  const originalRelations = responseRelations.filter((item) => item?.source && item?.target);
  const subprogramRelations = subprograms.flatMap((subprogram) => {
    const source = subprogram.qualifiedName
      || (packageName && `${packageName}.${subprogram.name}`)
      || subprogram.name;
    if (!source) return [];

    return [
      ...(subprogram.reads || []).map((target) => ({ source, target, relation: "READS" })),
      ...(subprogram.writes || []).map((target) => ({ source, target, relation: "WRITES" })),
      ...(subprogram.calls || []).map((target) => ({ source, target, relation: "CALLS" }))
    ].map((relation) => {
      const evidence = originalRelations.find((candidate) =>
        candidate.target === relation.target
        && candidate.relation?.toUpperCase() === relation.relation
      );
      return {
        ...evidence,
        ...relation,
        sourceObject: source,
        codeSnippet: evidence?.codeSnippet || subprogram.body || null
      };
    });
  });

  const explicitlyAttributed = originalRelations.filter(({ source, sourceObject }) =>
    nodeTypes.has(sourceObject || source) && (sourceObject || source) !== packageName
  );
  const validRelations = uniqueRelations(
    subprograms.length ? [...subprogramRelations, ...explicitlyAttributed] : originalRelations
  );
  const containsRelations = packageName ? subprograms.map((subprogram) => ({
    source: packageName,
    target: subprogram.qualifiedName || `${packageName}.${subprogram.name}`,
    relation: "CONTAINS"
  })) : [];

  validRelations.forEach(({ source, target, relation = "RELATED_TO" }) => {
    if (!nodeTypes.has(source)) nodeTypes.set(source, inferType(source, relation, "source"));
    if (!nodeTypes.has(target)) nodeTypes.set(target, inferType(target, relation, "target"));
  });

  const degree = new Map();
  [...containsRelations, ...validRelations].forEach(({ source, target }) => {
    degree.set(source, (degree.get(source) || 0) + 1);
    degree.set(target, (degree.get(target) || 0) + 1);
  });

  const nodes = [...nodeTypes].map(([id, rawType]) => {
    const nodeType = rawType.toUpperCase();
    const colors = NODE_STYLES[nodeType] || NODE_STYLES.UNKNOWN;
    const nodeRelations = validRelations.filter(
      ({ source, target, sourceObject }) =>
        source === id || target === id || sourceObject === id
    );
    const associatedSubprograms = subprograms.filter(({ qualifiedName }) =>
      id === packageName
        ? qualifiedName?.startsWith(`${id}.`)
        : qualifiedName === id
    );
    const reads = [...new Set(nodeRelations
      .filter(({ source, relation }) => source === id && relation?.toUpperCase() === "READS")
      .map(({ target }) => target))];
    const writes = [...new Set(nodeRelations
      .filter(({ source, relation }) => source === id && relation?.toUpperCase() === "WRITES")
      .map(({ target }) => target))];
    const accessedBy = nodeRelations
      .filter(({ target, relation }) => target === id && ["READS", "WRITES"].includes(relation?.toUpperCase()))
      .map(({ source, relation, sourceObject }) => ({
        name: sourceObject || source,
        relation: relation.toUpperCase()
      }));
    const codeSnippets = [...new Set(nodeRelations
      .map(({ codeSnippet }) => codeSnippet)
      .filter(Boolean))];

    return {
      id,
      position: { x: 0, y: 0 },
      data: {
        label: id,
        name: id,
        type: nodeType,
        nodeType,
        degree: degree.get(id) || 0,
        subprograms: associatedSubprograms,
        relations: nodeRelations,
        reads,
        writes,
        accessedBy,
        codeSnippets,
        bodySnippet: associatedSubprograms.find(({ qualifiedName }) => qualifiedName === id)?.body || null
      },
      style: {
        ...colors,
        color: "white",
        border: "2px solid #0f172a",
        padding: "12px",
        width: (degree.get(id) || 0) >= 4 ? 260 : 210,
        fontWeight: 700,
        boxShadow: "0 8px 20px rgba(0,0,0,0.3)"
      }
    };
  });

  const edges = [...containsRelations, ...validRelations].map((edge, index) => {
    const relation = (edge.relation || "RELATED_TO").toUpperCase();
    const color = RELATION_COLORS[relation] || "#94a3b8";
    return {
      id: `${relation}-${edge.source}-${edge.target}-${index}`,
      source: edge.source,
      target: edge.target,
      label: relation,
      data: { relation },
      markerEnd: { type: MarkerType.ArrowClosed, color },
      style: { stroke: color, strokeWidth: 2 },
      labelStyle: { fill: color, fontWeight: 700 }
    };
  });

  return getLayoutedElements(nodes, edges);
}
