import { useState } from "react";
import GraphView from "./components/GraphView";
import OraclePackageUploader from "./components/OraclePackageUploader";
import { getKnowledgeGraph } from "./services/api";

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
      console.log("GRAPH:", res.data);

      if (!res.data || !res.data.nodes) return;

      // Edges
      const flowEdges = (res.data.edges || []).map((edge, index) => ({
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
          glow = "0 0 30px rgba(239,68,68,0.8)"; // Red glowing
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
            transition: "all 0.3s ease",
            cursor: "pointer"
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
      <div style={{ flex: 1, position: "relative" }}>
        {screen === "graph" ? (
          <GraphView nodes={nodes} edges={edges} onNodeClick={handleNodeClick} />
        ) : (
          <OraclePackageUploader />
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