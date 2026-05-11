import { useState } from "react";

import GraphView from "./components/GraphView";
import OraclePackageUploader from "./components/OraclePackageUploader";

import { getKnowledgeGraph } from "./services/api";

function App() {

  const [screen, setScreen] = useState("graph");

  const [nodes, setNodes] = useState([]);
  const [edges, setEdges] = useState([]);

  // =====================================================
  // LOAD GRAPH
  // =====================================================

  const handleAnalyze = async () => {

    try {

      const res = await getKnowledgeGraph();

      console.log("GRAPH:", res.data);

      // =========================================
      // EDGES
      // =========================================

      const flowEdges = res.data.edges.map((edge, index) => ({

        id: `edge-${index}`,

        source: edge.source,

        target: edge.target,

        label: edge.relation,

        animated: true
      }));

      // =========================================
      // NODES
      // =========================================

      const uniqueNodes = [...new Set(res.data.nodes)];

      const flowNodes = uniqueNodes.map((node, index) => ({

        id: node,

        position: {

          x: 200 + (index % 4) * 300,

          y: 100 + Math.floor(index / 4) * 180
        },

        data: {

          label: node
        },

        style: {

          background: "#2563eb",

          color: "white",

          border: "2px solid #1e293b",

          borderRadius: "12px",

          padding: "10px",

          width: 180,

          fontWeight: "bold",

          boxShadow: "0 8px 20px rgba(0,0,0,0.25)"
        }
      }));

      setNodes(flowNodes);

      setEdges(flowEdges);

    } catch (err) {

      console.error("ERROR:", err);
    }
  };

  // =====================================================
  // UI
  // =====================================================

  return (

    <div
      style={{
        display: "flex",
        height: "100vh",
        background: "#0f172a",
        color: "white",
        overflow: "hidden"
      }}
    >

      {/* ========================================= */}
      {/* SIDEBAR */}
      {/* ========================================= */}

      <div
        style={{
          width: "260px",
          background: "#111827",
          padding: "20px",
          display: "flex",
          flexDirection: "column",
          gap: "14px",
          borderRight: "1px solid #1f2937"
        }}
      >

        <div>

          <h1
            style={{
              fontSize: "32px",
              marginBottom: "5px"
            }}
          >
            LegacyMind
          </h1>

          <p
            style={{
              color: "#9ca3af",
              fontSize: "14px"
            }}
          >
            AI-Powered Oracle Legacy Analyzer
          </p>

        </div>

        <hr
          style={{
            borderColor: "#1f2937",
            width: "100%"
          }}
        />

        <button
          onClick={handleAnalyze}
          style={buttonStyle("#2563eb")}
        >
          Analyze Graph
        </button>

        <button
          onClick={() => setScreen("graph")}
          style={buttonStyle("#059669")}
        >
          Open Graph
        </button>

        <button
          onClick={() => setScreen("upload")}
          style={buttonStyle("#7c3aed")}
        >
          Upload Packages
        </button>

        <div
          style={{
            marginTop: "25px",
            background: "#0f172a",
            borderRadius: "14px",
            padding: "15px"
          }}
        >

          <h3>System Stats</h3>

          <p>Nodes: {nodes.length}</p>

          <p>Relations: {edges.length}</p>

        </div>

      </div>

      {/* ========================================= */}
      {/* MAIN CONTENT */}
      {/* ========================================= */}

      <div
        style={{
          flex: 1,
          position: "relative"
        }}
      >

        {
          screen === "graph"

            ?

            <GraphView
              nodes={nodes}
              edges={edges}
              onNodeClick={() => {}}
            />

            :

            <OraclePackageUploader />
        }

      </div>

    </div>
  );
}

// =====================================================
// BUTTON STYLE
// =====================================================

function buttonStyle(background) {

  return {

    padding: "14px",

    borderRadius: "12px",

    border: "none",

    background,

    color: "white",

    cursor: "pointer",

    fontWeight: "bold",

    fontSize: "14px",

    transition: "0.2s"
  };
}

export default App;