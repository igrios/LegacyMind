import { useState } from "react";
import GraphView from "./components/GraphView";
import OraclePackageUploader from "./components/OraclePackageUploader";
import { getKnowledgeGraph } from "./services/api";
import { buildKnowledgeGraph } from "./utils/knowledgeGraph";

function App() {
  const [screen, setScreen] = useState("graph");
  const [nodes, setNodes] = useState([]);
  const [edges, setEdges] = useState([]);
  const [selectedNode, setSelectedNode] = useState(null);

  // Base URL unificada del backend para evitar pegar a producción en local
  const API_BASE_URL = process.env.REACT_APP_API_URL || "https://legacymind-api.onrender.com/api/legacy";

  // =====================================================
  // LOAD GRAPH
  // =====================================================
  const handleAnalyze = async () => {
    try {
      const res = await getKnowledgeGraph();
      const graph = buildKnowledgeGraph(res.data);
      setNodes(graph.nodes);
      setEdges(graph.edges);
    } catch (err) {
      console.error("Error loading knowledge graph:", err);
    }
  };

  const handleAnalysisComplete = (analysis) => {
    const graph = buildKnowledgeGraph(analysis);
    setNodes(graph.nodes);
    setEdges(graph.edges);
    setScreen("graph");
  };

  // =====================================================
  // CLEAR DATABASE
  // =====================================================
  const handleClearDatabase = async () => {
    const confirmed = window.confirm("¿Seguro que querés borrar todos los análisis?");
    if (!confirmed) return;

    try {
      const response = await fetch(`${API_BASE_URL}/database`, {
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
  const handleNodeClick = async (nodeId) => {
    try {
      const response = await fetch(`${API_BASE_URL}/object/${nodeId}`);
      if (!response.ok) throw new Error("Error fetching object details");

      const details = await response.json();
      setSelectedNode(details);
    } catch (error) {
      console.error("Error loading node details:", error);
    }
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
          <h1 style={{ fontSize: "38px", marginBottom: "6px", fontWeight: "800" }}>LegacyMind</h1>
          <p style={{ color: "#94a3b8", fontSize: "13px" }}>AI-Powered Oracle Legacy Analyzer</p>
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
            marginTop: "auto",
            background: "#0f172a",
            borderRadius: "16px",
            padding: "18px",
            border: "1px solid #1f2937"
          }}
        >
          <h3 style={{ fontSize: "16px", marginBottom: "8px" }}>System Stats</h3>
          <p style={{ fontSize: "14px", margin: "4px 0" }}>Nodes: {nodes.length}</p>
          <p style={{ fontSize: "14px", margin: "4px 0" }}>Relations: {edges.length}</p>
        </div>
      </div>

      {/* MAIN VIEW AREA */}
      <div style={{ flex: 1, minWidth: 0, minHeight: 0, position: "relative" }}>
        {screen === "graph" ? (
          <GraphView nodes={nodes} edges={edges} onNodeClick={handleNodeClick} />
        ) : (
          <OraclePackageUploader onAnalysisComplete={handleAnalysisComplete} />
        )}
      </div>

      {/* RIGHT METRICS PANEL (Unificado y blindado de crasheos) */}
      {selectedNode && (
        <div
          style={{
            width: "340px",
            background: "#111827",
            borderLeft: "1px solid #1f2937",
            padding: "22px",
            overflow: "auto",
            display: "flex",
            flexDirection: "column",
            gap: "20px"
          }}
        >
          <div
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center"
            }}
          >
            <h2 style={{ fontSize: "20px", wordBreak: "break-all", marginRight: "10px" }}>
              {selectedNode.name}
            </h2>
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

          <div style={{ fontSize: "14px", display: "flex", flexDirection: "column", gap: "6px" }}>
            <p><strong>Type:</strong> {selectedNode.type}</p>
            <p><strong>Risk Level:</strong> {selectedNode.riskLevel || "LOW"}</p>
            <p><strong>Risk Score:</strong> {selectedNode.riskScore || 0}</p>
          </div>

          {/* BUSINESS RULES */}
          {selectedNode.businessRules?.length > 0 && (
            <div>
              <h3 style={{ fontSize: "16px", marginBottom: "10px", color: "#38bdf8" }}>Business Rules</h3>
              {selectedNode.businessRules.map(rule => (
                <div
                  key={rule.errorCode}
                  style={{
                    background: "#0f172a",
                    borderRadius: "12px",
                    padding: "12px",
                    marginBottom: "10px",
                    border: "1px solid #1f2937"
                  }}
                >
                  <strong style={{ color: "#f43f5e" }}>{rule.errorCode}</strong>
                  <p style={{ fontSize: "13px", marginTop: "4px", color: "#cbd5e1" }}>{rule.message}</p>
                </div>
              ))}
            </div>
          )}

          {/* REFERENCED TABLES */}
          {selectedNode.referencedTables?.length > 0 && (
            <div>
              <h3 style={{ fontSize: "16px", marginBottom: "10px", color: "#38bdf8" }}>Referenced Tables</h3>
              <ul style={{ paddingLeft: "20px", fontSize: "14px", color: "#cbd5e1" }}>
                {selectedNode.referencedTables.map(table => (
                  <li key={table} style={{ marginBottom: "4px" }}>{table}</li>
                ))}
              </ul>
            </div>
          )}

          {/* CODE SMELLS (Se movió ADENTRO del bloque seguro para que no rompa) */}
          {selectedNode.codeSmells?.length > 0 && (
            <div>
              <h3 style={{ fontSize: "16px", marginBottom: "10px", color: "#38bdf8" }}>Code Smells</h3>
              <ul style={{ paddingLeft: "20px", fontSize: "14px", color: "#f43f5e" }}>
                {selectedNode.codeSmells.map(smell => (
                  <li key={smell} style={{ marginBottom: "6px" }}>{smell}</li>
                ))}
              </ul>
            </div>
          )}
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
    transition: "all 0.2s ease"
  };
}

export default App;
