import { useState } from "react";

import ControlPanel from "./components/ControlPanel";
import GraphView from "./components/GraphView";
import SidePanel from "./components/SidePanel";
import getLayoutedElements from "./utils/layout";
import { getKnowledgeGraph } from "./services/api";

function App() {

  const [nodes, setNodes] = useState([]);

  const [edges, setEdges] = useState([]);

  const [selectedNode, setSelectedNode] = useState(null);

  // 🔥 Cargar Knowledge Graph persistido
  const handleAnalyze = async () => {

    try {

      console.log("Cargando Knowledge Graph...");

      const res = await getKnowledgeGraph();

      console.log("GRAPH:", res.data);

      // 🔵 Nodos
      const flowNodes = res.data.nodes.map((node, index) => ({

        id: node,

        data: {
          label: node
        },

        position: {
          x: index * 250,
          y: 150
        }
      }));

      // 🔗 Edges
      const flowEdges = res.data.edges.map((edge, index) => ({

        id: `edge-${index}`,

        source: edge.source,

        target: edge.target,

        label: edge.relation,

        animated: true
      }));

    const layouted =
  getLayoutedElements(
    flowNodes,
    flowEdges
  );

setNodes(layouted.nodes);

setEdges(layouted.edges);
    } catch (err) {

      console.error("Error loading graph:", err);
    }
  };

  // 🖱 Click nodo
  const handleNodeClick = (nodeId) => {

    console.log("Nodo seleccionado:", nodeId);

    setSelectedNode(nodeId);
  };

  // 🔄 Reset
  const handleReset = () => {

    console.log("Reset graph");

    setNodes([]);

    setEdges([]);

    setSelectedNode(null);
  };

  return (

    <div>

      <ControlPanel
        onAnalyze={handleAnalyze}
        onReset={handleReset}
      />

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