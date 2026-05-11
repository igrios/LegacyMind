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
        width: "100%",
        height: "100vh",
        background: "#0f172a"
      }}
    >

      <ReactFlow

        nodes={nodes}

        edges={edges}

        fitView

        onNodeClick={(event, node) => {

          onNodeClick(node.id);
        }}

      >

        <MiniMap />

        <Controls />

        <Background />

      </ReactFlow>

    </div>
  );
}