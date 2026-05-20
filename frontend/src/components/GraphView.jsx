import ReactFlow, {
  Background,
  Controls,
  MiniMap
} from "reactflow";

import "reactflow/dist/style.css";

export default function GraphView({
  nodes,
  edges,
  onNodeClick
}) {

  console.log("GRAPH NODES:", nodes);

  console.log("GRAPH EDGES:", edges);

  return (

    <div
      style={{
        width: "100%",
        height: "100vh",
        background: "#020617"
      }}
    >

      <ReactFlow
        nodes={nodes}
        edges={edges}
        fitView
        nodesDraggable={true}
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