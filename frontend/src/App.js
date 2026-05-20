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

  // NUEVO STATE
  const [backendReady, setBackendReady] = useState(false);

  // =====================================================
  // BACKEND HEALTH CHECK
  // =====================================================

  useEffect(() => {

    const checkBackend = async () => {

      try {

        const response = await fetch(
          "https://legacymind-api.onrender.com/api/system/health"
        );

        if (response.ok) {

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
          background: "#0f172a",
          color: "white",
          fontFamily: "Arial"
        }}
      >

        <h1>🔥 Warming LegacyMind Backend</h1>

        <p>Initializing semantic engine...</p>

        <p>Connecting PostgreSQL...</p>

        <p>Loading dependency graph...</p>

      </div>
    );
  }

  // =====================================================
  // LOAD GRAPH
  // =====================================================
  const handleAnalyze = async () => {
    try {
      const res = await getKnowledgeGraph();
      console.log("GRAPH:", res.data);

      // Edges
      const flowEdges = res.data.edges.map((edge, index) => ({
        id: `edge-${index}`,
        source: edge.source,
        target: edge.target,
        label: edge.relation,
        relation: edge.relation,
        animated: true
      }));

      // Degree Map
      const degreeMap = {};
      flowEdges.forEach(edge => {
        degreeMap[edge.source] = (degreeMap[edge.source] || 0) + 1;
        degreeMap[edge.target] = (degreeMap[edge.target] || 0) + 1;
      });

      // Unique Nodes
      const uniqueNodes = [...new Set(res.data.nodes)];

      // Build Nodes
      const flowNodes = uniqueNodes.map((node, index) => {
        // Node Type Detection
        let nodeType = "TABLE";
        if (node.startsWith("PKG_")) nodeType = "PACKAGE";
        else if (node.startsWith("SP_")) nodeType = "PROCEDURE";
        else if (node.startsWith("TRG_")) nodeType = "TRIGGER";
        else if (node.startsWith("VW_") || node.startsWith("V_")) nodeType = "VIEW";

        // Color Mapping
        let background = "#22c55e"; // Table default
        if (nodeType === "PACKAGE") background = "#2563eb";
        else if (nodeType === "PROCEDURE") background = "#06b6d4";
        else if (nodeType === "TRIGGER") background = "#f97316";
        else if (nodeType === "VIEW") background = "#a855f7";

        // Criticality / Sizing based on degree
        const degree = degreeMap[node] || 0;
        let width = 180;
        let height = 55;
        let fontSize = "14px";
        let glow = "0 8px 20px rgba(0,0,0,0.25)";

        if (degree >= 6) {
          width = 320;
          height = 95;
          fontSize = "20px";
          glow = "0 0 30px rgba(239,68,68,0.8)"; // Red glowing for highly coupled nodes
        } else if (degree >= 3) {
          width = 250;
          height = 75;
          fontSize = "16px";
        }

        return {
          id: node,
          position: {
            x: 150 + (index % 4) * 320,
            y: 120 + Math.floor(index / 4) * 220
          },
          data: {
            label: node,
            nodeType,
            degree
          },
          style: {
            background,
            color: "white",
            border: "2px solid #111827",
            borderRadius: "16px",
            padding: "10px",
            width,
            height,
            fontWeight: "bold",
            fontSize,
            boxShadow: glow,
            transition: "all 0.3s ease"
          }
        };
      });

      setNodes(flowNodes);
      setEdges(flowEdges);
    } catch (err) {
      console.error("Error loading knowledge graph:", err);
    }
  };

  // =====================================================
  // CLEAR DATABASE
  // =====================================================
  const handleClearDatabase = async () => {
    const confirmed = window.confirm("¿Seguro que querés borrar todos los análisis?");
    if (!confirmed) return;

    try {
      // ✅ ARREGLADO: URL limpia y formateada sin código duplicado
      const response = await fetch("https://legacymind-api.onrender.com/api/legacy/database", {
        method: "DELETE"
      });

      if (!response.ok) {
        throw new Error("Error cleaning database");
      }

      alert("Base limpiada correctamente");
      setNodes([]);
      setEdges([]);
      setSelectedNode(null);
    } catch (err) {
      console.error(err);
      alert("Error al borrar análisis");
    }
  };

  // =====================================================
  // NODE CLICK HANDLER
  // =====================================================
  const handleNodeClick = (nodeId) => {
    const node = nodes.find(n => n.id === nodeId);
    if (!node) return;

    const relatedEdges = edges.filter(
      edge => edge.source === nodeId || edge.target === nodeId
    );

    setSelectedNode({
      ...node,
      relatedEdges
    });
  };

  // =====================================================
  // VIEW RENDER
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
      {/* SIDEBAR */}
      <div
        style={{
          width: "280px",
          background: "#111827",
          borderRight: "1px solid #1f2937",
          padding: "20px",
          display: "flex",
          flexDirection: "column",
          gap: "14px"
        }}
      >
        <div>
          <h1 style={{ fontSize: "42px", marginBottom: "6px" }}>LegacyMind</h1>
          <p style={{ color: "#94a3b8" }}>AI-Powered Oracle Legacy Analyzer</p>
        </div>

        <hr style={{ borderColor: "#1f2937", width: "100%" }} />

        <button onClick={handleAnalyze} style={buttonStyle("#2563eb")}>
          Analyze Graph
        </button>

        <button
          onClick={() => {
            setScreen("graph");
            handleAnalyze();
          }}
          style={buttonStyle("#059669")}
        >
          Open Graph
        </button>

        <button onClick={handleClearDatabase} style={buttonStyle("#dc2626")}>
          Borrar análisis
        </button>

        <button onClick={() => setScreen("upload")} style={buttonStyle("#7c3aed")}>
          Upload Packages
        </button>

        {/* STATS PANEL */}
        <div
          style={{
            marginTop: "30px",
            background: "#0f172a",
            borderRadius: "16px",
            padding: "18px"
          }}
        >
          <h3>System Stats</h3>
          <p>Nodes: {nodes.length}</p>
          <p>Relations: {edges.length}</p>
        </div>
      </div>

      {/* MAIN VIEW AREA */}
      <div style={{ flex: 1, position: "relative" }}>
        {screen === "graph" ? (
          <GraphView nodes={nodes} edges={edges} onNodeClick={handleNodeClick} />
        ) : (
          <OraclePackageUploader />
        )}
      </div>

      {/* RIGHT METRICS PANEL */}
      {selectedNode && (
        <div
          style={{
            width: "320px",
            background: "#111827",
            borderLeft: "1px solid #1f2937",
            padding: "22px",
            overflow: "auto"
          }}
        >
          <div
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              marginBottom: "20px"
            }}
          >
            <h2 style={{ fontSize: "24px" }}>{selectedNode.data.label}</h2>
            <button
              onClick={() => setSelectedNode(null)}
              style={{
                background: "#dc2626",
                border: "none",
                color: "white",
                borderRadius: "8px",
                padding: "8px 12px",
                cursor: "pointer",
                fontWeight: "bold"
              }}
            >
              ✕
            </button>
          </div>

          <p><strong>Type:</strong> {selectedNode.data.nodeType}</p>
          <p><strong>Criticality:</strong> {selectedNode.data.degree}</p>
          <p><strong>Relations:</strong> {selectedNode.relatedEdges.length}</p>

          <hr style={{ marginTop: "20px", marginBottom: "20px", borderColor: "#1f2937" }} />

          <h3 style={{ marginBottom: "15px" }}>Connected Relations</h3>

          {selectedNode.relatedEdges.map((edge, index) => (
            <div
              key={index}
              style={{
                background: "#0f172a",
                borderRadius: "14px",
                padding: "14px",
                marginBottom: "12px"
              }}
            >
              <strong>{edge.relation}</strong>
              <br />
              {`${edge.source} → ${edge.target}`}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

// =====================================================
// BUTTON STYLE REUSABLE COMPONENT
// =====================================================
function buttonStyle(background) {
  return {
    padding: "14px",
    borderRadius: "14px",
    border: "none",
    background,
    color: "white",
    cursor: "pointer",
    fontWeight: "bold",
    fontSize: "15px",
    transition: "background 0.2s ease"
  };
}

export default App;