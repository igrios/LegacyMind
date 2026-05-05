import dagre from "dagre";

export default function mapToFlow(graph) {
  if (!graph || !graph.nodes) {
    return { nodes: [], edges: [] };
  }

  const g = new dagre.graphlib.Graph();
  g.setDefaultEdgeLabel(() => ({}));

  g.setGraph({
    rankdir: "TB", // 👈 vertical (root arriba)
    nodesep: 60,
    ranksep: 120,
  });

  const nodeWidth = 160;
  const nodeHeight = 50;

  const root = graph.name;

  // 🧠 aseguramos que root esté primero
  const allNodes = [root, ...graph.nodes];

  // 👉 NODOS
  allNodes.forEach((node) => {
    g.setNode(node, {
      width: nodeWidth,
      height: nodeHeight,
      rank: node === root ? 0 : undefined // 🔥 root arriba SIEMPRE
    });
  });

  // 👉 EDGES
  (graph.edges || []).forEach((edge) => {
    g.setEdge(edge.from, edge.to);
  });

  dagre.layout(g);

  // 🔥 centrar grafo
  const positions = allNodes.map((n) => g.node(n));

  const minX = Math.min(...positions.map((p) => p.x));
  const minY = Math.min(...positions.map((p) => p.y));

  const offsetX = -minX + 100;
  const offsetY = -minY + 100;

  const nodes = allNodes.map((node) => {
    const pos = g.node(node);

    return {
      id: node,
      data: { label: node },
      position: {
        x: pos.x + offsetX,
        y: pos.y + offsetY,
      },
      style: {
        background: node === root ? "#111" : "#e5e7eb",
        color: node === root ? "#fff" : "#000",
        fontWeight: node === root ? "bold" : "normal",
        border: node === root ? "3px solid #000" : "1px solid #333",
        borderRadius: "6px",
        padding: "6px",
      },
    };
  });

  const edges = (graph.edges || []).map((edge, i) => ({
    id: `${edge.from}-${edge.to}-${i}`,
    source: edge.from,
    target: edge.to,
    style: {
      stroke: edge.from === root ? "#000" : "#888",
      strokeWidth: edge.from === root ? 2.5 : 1.5,
    },
    animated: edge.from === root
  }));

  return { nodes, edges };
}