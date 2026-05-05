import ReactFlow from "reactflow";
import "reactflow/dist/style.css";

export default function GraphView({ nodes, edges, onNodeClick }) {
  return (
    <div style={{ width: "100vw", height: "100vh" }}>
      <ReactFlow
        nodes={nodes}
        edges={edges}
        fitView
        onNodeClick={(event, node) => {
          console.log("CLICK:", node.id);
          onNodeClick(node.id);
        }}
      />
    </div>
  );
}