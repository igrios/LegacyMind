import { useState } from "react";

import GraphView from "./components/GraphView";
import OraclePackageUploader from "./components/OraclePackageUploader";

import { getKnowledgeGraph } from "./services/api";

function App() {

  const [nodes, setNodes] = useState([]);
  const [edges, setEdges] = useState([]);
  const [selectedNode, setSelectedNode] = useState(null);

  // =========================================
  // SCREEN
  // =========================================

  const [screen, setScreen] = useState("graph");

  // =========================================
  // ANALYZE GRAPH
  // =========================================

  const handleAnalyze = async () => {

    try {

      console.log("Loading Knowledge Graph...");

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

      // =========================================
      // CENTRALITY
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

      const flowNodes =

        res.data.nodes

          .filter(node => node != null)

          .map((node, index) => {

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
              node.startsWith("V_")
              ||
              node.startsWith("VW_")
            ) {

              nodeType = "VIEW";
            }

            else if (node.startsWith("DBMS_")) {

              nodeType = "SYSTEM";
            }

            else if (node.startsWith("REGLA_")) {

              nodeType = "RULE";
            }

            // =========================================
            // COLORS
            // =========================================

            let background = "#22c55e";

            if (nodeType === "PACKAGE") {

              background = "#3b82f6";
            }

            else if (nodeType === "PROCEDURE") {

              background = "#0ea5e9";
            }

            else if (nodeType === "TRIGGER") {

              background = "#f97316";
            }

            else if (nodeType === "VIEW") {

              background = "#a855f7";
            }

            else if (nodeType === "SYSTEM") {

              background = "#6b7280";
            }

            else if (nodeType === "RULE") {

              background = "#ef4444";
            }

            // =========================================
            // CRITICALITY
            // =========================================

            const degree = degreeMap[node] || 0;

            let width = 180;
            let height = 55;
            let borderWidth = 1;

            if (degree >= 6) {

              width = 320;
              height = 90;
              borderWidth = 5;
            }

            else if (degree >= 3) {

              width = 240;
              height = 70;
              borderWidth = 3;
            }

            return {

              id: node,

              position: {

                x: 150 + (index % 4) * 320,

                y: 100 + Math.floor(index / 4) * 220
              },

              data: {

                label: node,

                nodeType,

                degree
              },

              style: {

                background,

                color: "white",

                border: `${borderWidth}px solid #111`,

                borderRadius: "12px",

                padding: "10px",

                width,

                height,

                fontWeight: "bold",

                fontSize:
                  degree >= 6
                    ? "18px"
                    : "14px",

                boxShadow:

                  degree >= 6

                    ?

                    "0 0 20px rgba(255,0,0,0.5)"

                    :

                    "0 0 10px rgba(0,0,0,0.2)",

                transition: "all 0.3s ease"
              }
            };
          });

      setNodes(flowNodes);

      setEdges(flowEdges);

    } catch (err) {

      console.error("ERROR:", err);
    }
  };

  // =========================================
  // NODE CLICK
  // =========================================

  const handleNodeClick = (nodeId) => {

    console.log("NODE CLICK:", nodeId);

    const node = nodes.find(n => n.id === nodeId);

    if (!node) {

      return;
    }

    const relatedEdges = edges.filter(

      e =>

        e.source === nodeId

        ||

        e.target === nodeId
    );

    const connectedNodeIds = new Set();

    relatedEdges.forEach(edge => {

      connectedNodeIds.add(edge.source);

      connectedNodeIds.add(edge.target);
    });

    setNodes(prevNodes =>

      prevNodes.map(node => {

        const isHighlighted =
          connectedNodeIds.has(node.id);

        return {

          ...node,

          style: {

            ...node.style,

            opacity:
              isHighlighted
                ? 1
                : 0.15
          }
        };
      })
    );

    setSelectedNode({

      ...node,

      relatedEdges
    });
  };

  // =========================================
  // RESET
  // =========================================

  const resetHighlight = () => {

    setNodes(prevNodes =>

      prevNodes.map(node => ({

        ...node,

        style: {

          ...node.style,

          opacity: 1
        }
      }))
    );

    setSelectedNode(null);
  };

  // =========================================
  // UI
  // =========================================

  return (

    <div>

      <div

        style={{

          position: "absolute",

          zIndex: 1000,

          top: 20,

          left: 20,

          display: "flex",

          gap: "10px"
        }}
      >

        <button

          onClick={handleAnalyze}

          style={{

            padding: "10px",

            borderRadius: "8px",

            border: "none",

            background: "#111827",

            color: "white",

            cursor: "pointer"
          }}
        >

          Analyze

        </button>

        <button

          onClick={resetHighlight}

          style={{

            padding: "10px",

            borderRadius: "8px",

            border: "none",

            background: "#dc2626",

            color: "white",

            cursor: "pointer"
          }}
        >

          Reset

        </button>

        <button

          onClick={() => setScreen("upload")}

          style={{

            padding: "10px",

            borderRadius: "8px",

            border: "none",

            background: "#2563eb",

            color: "white",

            cursor: "pointer"
          }}
        >

          Carga

        </button>

        <button

          onClick={() => setScreen("graph")}

          style={{

            padding: "10px",

            borderRadius: "8px",

            border: "none",

            background: "#059669",

            color: "white",

            cursor: "pointer"
          }}
        >

          Grafo

        </button>

      </div>

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
  );
}

export default App;