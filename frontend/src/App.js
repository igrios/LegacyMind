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

  // Backend status
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

        if (response.ok) {

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

      const flowNodes = res.data.nodes.map((node) => ({
        id: node.id,
        data: {
          label: node.label
        },
        position: {
          x: Math.random() * 500,
          y: Math.random() * 500
        }
      }));

      setNodes(flowNodes);

    } catch (error) {

      console.error(error);
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
  // MAIN APP
  // =====================================================

  return (

    <div style={{ width: "100vw", height: "100vh" }}>

      <OraclePackageUploader onAnalyze={handleAnalyze} />

      <GraphView
        nodes={nodes}
        edges={edges}
        onNodeClick={setSelectedNode}
      />

    </div>
  );
}

export default App;