import ReactFlow, {
  Background,
  Controls,
  MiniMap,
  MarkerType
} from "reactflow";

import "reactflow/dist/style.css";

export default function GraphView({
  nodes,
  edges,
  onNodeClick
}) {

  console.log("GRAPH VIEW NODES:", nodes);
  console.log("GRAPH VIEW EDGES:", edges);

  // =====================================================
  // SAFE NODES
  // =====================================================

  const safeNodes = nodes.map(node => ({

    ...node,

    sourcePosition: "right",

    targetPosition: "left"
  }));

  // =====================================================
  // SAFE EDGES
  // =====================================================

  const safeEdges = edges.map(edge => ({

    ...edge,

    markerEnd: {
      type: MarkerType.ArrowClosed
    },

    style: {
      stroke: "#22c55e",
      strokeWidth: 2
    },

    labelStyle: {
      fill: "white",
      fontWeight: "bold"
    }
  }));

  return (

    <div
      style={{
        width: "100%",
        height: "100vh",
        background: "#020617"
      }}
    >

      <ReactFlow
        nodes={safeNodes}
        edges={safeEdges}
        fitView
        nodesDraggable
        nodesConnectable={false}
        elementsSelectable
        onNodeClick={(event, node) => {

          console.log("NODE CLICK:", node);

          if (onNodeClick) {
            onNodeClick(node.id);
          }
        }}
      >

        <Background />

        <Controls />

        <MiniMap />

      </ReactFlow>

    </div>
  );
}