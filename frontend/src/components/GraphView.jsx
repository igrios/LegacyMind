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

  return (

    <div
      style={{
        position: "absolute",
        inset: 0,
        width: "100%",
        height: "100%",
        minHeight: "400px",
        background: "#0f172a"
      }}
    >

      <ReactFlow

        nodes={nodes}

        edges={edges}

        fitView

        onNodeClick={(event, node) => {

          onNodeClick?.(node);
        }}

      >

        <MiniMap />

        <Controls />

        <Background />

      </ReactFlow>

    </div>
  );
}
