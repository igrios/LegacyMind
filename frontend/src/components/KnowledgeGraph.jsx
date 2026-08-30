import { useEffect, useState } from "react";

import ReactFlow, {
  Background,
  Controls,
  MiniMap
} from "reactflow";

import "reactflow/dist/style.css";
import SidePanel from "./SidePanel";
import { buildKnowledgeGraph } from "../utils/knowledgeGraph";

export default function KnowledgeGraph() {

  const [nodes, setNodes] = useState([]);
  const [edges, setEdges] = useState([]);
  const [selectedNode, setSelectedNode] = useState(null);

  useEffect(() => {

    const apiUrl = process.env.REACT_APP_API_URL || "https://legacymind-api.onrender.com/api/legacy";

    fetch(`${apiUrl}/knowledge-graph`)

      .then((response) => response.json())

      .then((data) => {

        const graph = buildKnowledgeGraph(data);
        setNodes(graph.nodes);
        setEdges(graph.edges);
      });

  }, []);

  return (

    <div style={{ position: "relative", width: "100%", height: "800px", minHeight: "400px" }}>

      <ReactFlow
        nodes={nodes}
        edges={edges}
        fitView
        onNodeClick={async (_, node) => {

          try {

            const response = await fetch(
              `${process.env.REACT_APP_API_URL || "https://legacymind-api.onrender.com/api/legacy"}/object/${node.id}`
            );

            const details = await response.json();

            console.log("DETAILS >>>", details);

            setSelectedNode(details);

          } catch (error) {

            console.error(error);
          }

        }}
      >

        <MiniMap />

        <Controls />

        <Background />

      </ReactFlow>

      <SidePanel node={selectedNode} />

    </div>
  );
}
