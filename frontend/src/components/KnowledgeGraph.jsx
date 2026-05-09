import { useEffect, useState } from "react";

import ReactFlow, {
  Background,
  Controls,
  MiniMap
} from "reactflow";

import "reactflow/dist/style.css";

export default function KnowledgeGraph() {

  const [nodes, setNodes] = useState([]);
  const [edges, setEdges] = useState([]);

  useEffect(() => {

    fetch("http://localhost:8080/api/legacy/knowledge-graph")

      .then((response) => response.json())

      .then((data) => {

        const flowNodes = data.nodes.map((node, index) => ({

          id: node,

          data: {
            label: node
          },

          position: {
            x: index * 250,
            y: 150
          }
        }));

        const flowEdges = data.edges.map((edge, index) => ({

          id: `edge-${index}`,

          source: edge.source,

          target: edge.target,

          label: edge.relation,

          animated: true
        }));

        setNodes(flowNodes);

        setEdges(flowEdges);
      });

  }, []);

  return (

    <div style={{ width: "100%", height: "800px" }}>

      <ReactFlow
        nodes={nodes}
        edges={edges}
        fitView
      >

        <MiniMap />

        <Controls />

        <Background />

      </ReactFlow>

    </div>
  );
}