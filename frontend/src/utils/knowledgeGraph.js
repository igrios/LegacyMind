import { MarkerType } from "reactflow";
import getLayoutedElements from "./layout";

const NODE_STYLES = {
  PACKAGE: { background: "#2563eb", borderRadius: "16px" },
  PROCEDURE: { background: "#06b6d4", borderRadius: "999px" },
  FUNCTION: { background: "#0891b2", borderRadius: "999px" },
  TRIGGER: { background: "#f97316", borderRadius: "10px" },
  VIEW: { background: "#a855f7", borderRadius: "8px" },
  TABLE: { background: "#16a34a", borderRadius: "4px" },
  UNKNOWN: { background: "#64748b", borderRadius: "10px" }
};

const RELATION_COLORS = {
  READS: "#38bdf8",
  WRITES: "#f97316",
  CALLS: "#c084fc"
};

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
  const relations = Array.isArray(payload.knowledgeRelations)
    ? payload.knowledgeRelations
    : Array.isArray(payload.edges) ? payload.edges : [];
  const declaredNodes = Array.isArray(payload.nodes) ? payload.nodes : [];
  const nodeTypes = new Map();

  declaredNodes.forEach((node) => {
    const id = typeof node === "string" ? node : node?.id;
    if (id) nodeTypes.set(id, typeof node === "string" ? "UNKNOWN" : node.type || "UNKNOWN");
  });

  const validRelations = relations.filter((item) => item?.source && item?.target);
  validRelations.forEach(({ source, target, relation = "RELATED_TO" }) => {
    if (!nodeTypes.has(source)) nodeTypes.set(source, inferType(source, relation, "source"));
    if (!nodeTypes.has(target)) nodeTypes.set(target, inferType(target, relation, "target"));
  });

  const degree = new Map();
  validRelations.forEach(({ source, target }) => {
    degree.set(source, (degree.get(source) || 0) + 1);
    degree.set(target, (degree.get(target) || 0) + 1);
  });

  const nodes = [...nodeTypes].map(([id, rawType]) => {
    const nodeType = rawType.toUpperCase();
    const colors = NODE_STYLES[nodeType] || NODE_STYLES.UNKNOWN;
    return {
      id,
      position: { x: 0, y: 0 },
      data: { label: id, nodeType, degree: degree.get(id) || 0 },
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

  const edges = validRelations.map((edge, index) => {
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
