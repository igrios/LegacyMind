import { useEffect, useState } from "react";

import ReactFlow, {
  Background,
  Controls,
  MiniMap
} from "reactflow";

import "reactflow/dist/style.css";
import SidePanel from "./SidePanel";

export default function KnowledgeGraph() {

  const [nodes, setNodes] = useState([]);
  const [edges, setEdges] = useState([]);
  const [selectedNode, setSelectedNode] = useState(null);

  useEffect(() => {

    fetch(`${process.env.REACT_APP_API_URL}/knowledge-graph`)

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
        onNodeClick={async (_, node) => {

          try {

            const response = await fetch(
              `${process.env.REACT_APP_API_URL}/object/${node.id}`
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