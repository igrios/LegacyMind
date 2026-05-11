import { useState } from "react";

import GraphView from "./components/GraphView";
import OraclePackageUploader from "./components/OraclePackageUploader";

import { getKnowledgeGraph } from "./services/api";

function App() {

  const [screen, setScreen] = useState("graph");

  const [nodes, setNodes] = useState([]);

  const [edges, setEdges] = useState([]);

  const [selectedNode, setSelectedNode] = useState(null);

  // =====================================================
  // ANALYZE GRAPH
  // =====================================================

  const handleAnalyze = async () => {

    try {

      const res = await getKnowledgeGraph();

      console.log(res.data);

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

      // =========================================
      // DEGREE MAP
      // =========================================

      const degreeMap = {};

      flowEdges.forEach(edge => {

        degreeMap[edge.source] =
          (degreeMap[edge.source] || 0) + 1;

        degreeMap[edge.target] =
          (degreeMap[edge.target] || 0) + 1;
      });

      // =========================================
      // NODES
      // =========================================

      const uniqueNodes = [...new Set(res.data.nodes)];

      const flowNodes = uniqueNodes.map((node, index) => {

        // =====================================
        // TYPE
        // =====================================

        let nodeType = "TABLE";

        if (node.startsWith("PKG_")) {

          nodeType = "PACKAGE";
        }

        else if (node.startsWith("SP_")) {

          nodeType = "PROCEDURE";
        }

        else if (node.startsWith("TRG_")) {

          nodeType = "TRIGGER";
        }

        else if (

          node.startsWith("VW_")

          ||

          node.startsWith("V_")
        ) {

          nodeType = "VIEW";
        }

        // =====================================
        // COLORS
        // =====================================

        let background = "#22c55e";

        if (nodeType === "PACKAGE") {

          background = "#2563eb";
        }

        else if (nodeType === "PROCEDURE") {

          background = "#06b6d4";
        }

        else if (nodeType === "TRIGGER") {

          background = "#f97316";
        }

        else if (nodeType === "VIEW") {

          background = "#a855f7";
        }

        // =====================================
        // CRITICALITY
        // =====================================

        const degree = degreeMap[node] || 0;

        let width = 180;

        let height = 55;

        let fontSize = "14px";

        let glow = "0 8px 20px rgba(0,0,0,0.25)";

        if (degree >= 6) {

          width = 320;

          height = 95;

          fontSize = "20px";

          glow =
            "0 0 30px rgba(239,68,68,0.8)";
        }

        else if (degree >= 3) {

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

      console.error(err);
    }
  };

  // =====================================================
  // NODE CLICK
  // =====================================================

  const handleNodeClick = (nodeId) => {

    const node = nodes.find(n => n.id === nodeId);

    if (!node) return;

    const relatedEdges = edges.filter(

      edge =>

        edge.source === nodeId

        ||

        edge.target === nodeId
    );

    setSelectedNode({

      ...node,

      relatedEdges
    });
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

          <h1
            style={{
              fontSize: "42px",
              marginBottom: "6px"
            }}
          >
            LegacyMind
          </h1>

          <p
            style={{
              color: "#94a3b8"
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

      {/* ========================================= */}
      {/* MAIN */}
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
              onNodeClick={handleNodeClick}
            />

            :

            <OraclePackageUploader />
        }

      </div>

      {/* ========================================= */}
      {/* RIGHT PANEL */}
      {/* ========================================= */}

      {

        selectedNode && (

          <div
            style={{
              width: "340px",
              background: "#111827",
              borderLeft: "1px solid #1f2937",
              padding: "22px",
              overflow: "auto"
            }}
          >

            <h2
              style={{
                marginBottom: "10px"
              }}
            >
              {selectedNode.data.label}
            </h2>

            <p>
              <strong>Type:</strong>
              {" "}
              {selectedNode.data.nodeType}
            </p>

            <p>
              <strong>Criticality:</strong>
              {" "}
              {selectedNode.data.degree}
            </p>

            <p>
              <strong>Relations:</strong>
              {" "}
              {selectedNode.relatedEdges.length}
            </p>

            <hr
              style={{
                marginTop: "20px",
                marginBottom: "20px",
                borderColor: "#1f2937"
              }}
            />

            <h3
              style={{
                marginBottom: "15px"
              }}
            >
              Connected Relations
            </h3>

            {

              selectedNode.relatedEdges.map(

                (edge, index) => (

                  <div
                    key={index}
                    style={{
                      background: "#0f172a",
                      borderRadius: "14px",
                      padding: "14px",
                      marginBottom: "12px"
                    }}
                  >

                    <strong>
                      {edge.relation}
                    </strong>

                    <br />

                    {edge.source}

                    {" → "}

                    {edge.target}

                  </div>
                )
              )
            }

          </div>
        )
      }

    </div>
  );
}

// =====================================================
// BUTTON STYLE
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

    transition: "0.2s"
  };
}

export default App;