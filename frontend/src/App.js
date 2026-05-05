import { useState } from "react";
import ControlPanel from "./components/ControlPanel";
import GraphView from "./components/GraphView";
import SidePanel from "./components/SidePanel";
import { getGraph } from "./services/api";
import mapToFlow from "./utils/layout";

function App() {
  const [nodes, setNodes] = useState([]);
  const [edges, setEdges] = useState([]);
  const [expandedNodes, setExpandedNodes] = useState(new Set());
  const [selectedNode, setSelectedNode] = useState(null);

  // 🔵 Analizar tabla inicial
  const handleAnalyze = async (table) => {
    try {
      console.log("Analizando:", table);

      const res = await getGraph(table);
      console.log("DATA:", res.data);

      const result = mapToFlow(res.data, table, new Set([table]));

      setNodes(result.nodes);
      setEdges(result.edges);

      setExpandedNodes(new Set([table]));
      setSelectedNode(table);
    } catch (err) {
      console.error("Error analyze:", err);
    }
  };

  // 🔥 Click en nodo → expandir
  const handleNodeClick = async (nodeId) => {
    setSelectedNode(nodeId);

    // 🚫 evitar repetir requests
    if (expandedNodes.has(nodeId)) {
      console.log("Ya expandido:", nodeId);
      return;
    }

    try {
      console.log("Expandiendo:", nodeId);

      const res = await getGraph(nodeId);

      const result = mapToFlow(res.data, nodeId, expandedNodes);

      // 🧩 merge nodos
      setNodes((prevNodes) => {
        const existingIds = new Set(prevNodes.map((n) => n.id));
        return [
          ...prevNodes,
          ...result.nodes.filter((n) => !existingIds.has(n.id)),
        ];
      });

      // 🔗 merge edges
      setEdges((prevEdges) => {
        const existingIds = new Set(prevEdges.map((e) => e.id));
        return [
          ...prevEdges,
          ...result.edges.filter((e) => !existingIds.has(e.id)),
        ];
      });

      // ✔ marcar como expandido
      setExpandedNodes((prev) => {
        const updated = new Set(prev);
        updated.add(nodeId);
        return updated;
      });
    } catch (err) {
      console.error("Error expandiendo:", err);
    }
  };

  // 🔄 Reset completo
  const handleReset = () => {
    console.log("Reset grafo");

    setNodes([]);
    setEdges([]);
    setExpandedNodes(new Set());
    setSelectedNode(null);
  };

  return (
    <div>
      <ControlPanel onAnalyze={handleAnalyze} onReset={handleReset} />

      <GraphView
        nodes={nodes}
        edges={edges}
        onNodeClick={handleNodeClick}
      />

      <SidePanel node={selectedNode} />
    </div>
  );
}

export default App;