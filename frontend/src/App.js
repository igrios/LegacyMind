import { useEffect, useState } from "react";
import GraphView from "./components/GraphView";
import OraclePackageUploader from "./components/OraclePackageUploader";
import { getKnowledgeGraph } from "./services/api";

function App() {

  // =====================================================
  // STATES
  // =====================================================

  const [screen, setScreen] = useState("graph");
  const [nodes, setNodes] = useState([]);
  const [edges, setEdges] = useState([]);
  const [selectedNode, setSelectedNode] = useState(null);
  const [backendReady, setBackendReady] = useState(false);

  // =====================================================
  // BACKEND HEALTH CHECK
  // =====================================================

  useEffect(() => {

    const checkBackend = async () => {

      try {

        console.log("Checking backend...");

        const response = await fetch(
          "https://legacymind-api.onrender.com/api/system/health"
        );

        console.log("STATUS:", response.status);

        if (response.status === 200) {

          console.log("Backend READY");

          setBackendReady(true);
        }

      } catch (error) {

        console.log("Backend warming...");
      }
    };

    // Primera llamada
    checkBackend();

    // Polling cada 3 segundos
    const interval = setInterval(checkBackend, 3000);

    return () => clearInterval(interval);

  }, []);

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
        relation: edge.relation,
        animated: true
      }));

      setEdges(flowEdges);

      // =========================================
      // NODES
      // =========================================

      const flowNodes = res.data.nodes.map((node, index) => ({
        id: node.id,
        data: {
          label: node.label
        },
        position: {
          x: (index % 5) * 250,
          y: Math.floor(index / 5) * 180
        }
      }));

      setNodes(flowNodes);

    } catch (error) {

      console.error("GRAPH ERROR:", error);
    }
  };

  // =====================================================
  // WARMING SCREEN
  // =====================================================

  if (!backendReady) {

    return (

      <div
        style={{
          height: "100vh",
          display: "flex",
          flexDirection: "column",
          justifyContent: "center",
          alignItems: "center",
          background: "#020617",
          color: "white",
          fontFamily: "Arial"
        }}
      >

        <h1 style={{ fontSize: "48px", marginBottom: "30px" }}>
          🔥 Warming LegacyMind Backend
        </h1>

        <p style={{ margin: "10px" }}>
          Initializing semantic engine...
        </p>

        <p style={{ margin: "10px" }}>
          Connecting PostgreSQL...
        </p>

        <p style={{ margin: "10px" }}>
          Loading dependency graph...
        </p>

      </div>
    );
  }

  // =====================================================
  // MAIN APP
  // =====================================================

  return (

    <div
      style={{
        width: "100vw",
        height: "100vh",
        background: "#020617",
        color: "white",
        display: "flex",
        flexDirection: "column"
      }}
    >

      {/* ================================================= */}
      {/* TOP BAR */}
      {/* ================================================= */}

      <div
        style={{
          height: "70px",
          background: "#0f172a",
          borderBottom: "1px solid #1e293b",
          display: "flex",
          alignItems: "center",
          padding: "0 20px",
          gap: "15px"
        }}
      >

        <h2 style={{ marginRight: "20px" }}>
          🧠 LegacyMind
        </h2>

        <button
          onClick={() => setScreen("graph")}
          style={{
            padding: "10px 16px",
            background: screen === "graph" ? "#2563eb" : "#1e293b",
            color: "white",
            border: "none",
            borderRadius: "8px",
            cursor: "pointer"
          }}
        >
          Graph
        </button>

        <button
          onClick={() => setScreen("upload")}
          style={{
            padding: "10px 16px",
            background: screen === "upload" ? "#2563eb" : "#1e293b",
            color: "white",
            border: "none",
            borderRadius: "8px",
            cursor: "pointer"
          }}
        >
          Upload
        </button>

      </div>

      {/* ================================================= */}
      {/* CONTENT */}
      {/* ================================================= */}

      <div
        style={{
          flex: 1,
          display: "flex",
          overflow: "hidden"
        }}
      >

        {/* ============================================= */}
        {/* LEFT PANEL */}
        {/* ============================================= */}

        <div
          style={{
            width: "320px",
            background: "#0f172a",
            borderRight: "1px solid #1e293b",
            padding: "20px",
            overflowY: "auto"
          }}
        >

          <h3>⚡ Controls</h3>

          <div style={{ marginTop: "20px" }}>
            <OraclePackageUploader onAnalyze={handleAnalyze} />
          </div>

          {selectedNode && (

            <div style={{ marginTop: "30px" }}>

              <h3>📌 Selected Node</h3>

              <pre
                style={{
                  background: "#111827",
                  padding: "10px",
                  borderRadius: "8px",
                  overflow: "auto"
                }}
              >
                {JSON.stringify(selectedNode, null, 2)}
              </pre>

            </div>
          )}

        </div>

        {/* ============================================= */}
        {/* GRAPH AREA */}
        {/* ============================================= */}

        <div
          style={{
            flex: 1,
            position: "relative"
          }}
        >

          <GraphView
            nodes={nodes}
            edges={edges}
            onNodeClick={setSelectedNode}
          />

        </div>

      </div>

    </div>
  );
}

export default App;