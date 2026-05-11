import { useState } from "react";

import GraphView from "./components/GraphView";
import OraclePackageUploader from "./components/OraclePackageUploader";

function App() {

  const [screen, setScreen] = useState("graph");

  return (

    <div
      style={{
        display: "flex",
        height: "100vh",
        background: "#0f172a",
        color: "white"
      }}
    >

      {/* SIDEBAR */}

      <div
        style={{
          width: "260px",
          background: "#111827",
          padding: "20px",
          display: "flex",
          flexDirection: "column",
          gap: "12px"
        }}
      >

        <h1
          style={{
            fontSize: "28px",
            marginBottom: "10px"
          }}
        >
          LegacyMind
        </h1>

        <button
          onClick={() => setScreen("graph")}
          style={buttonStyle("#2563eb")}
        >
          Graph
        </button>

        <button
          onClick={() => setScreen("upload")}
          style={buttonStyle("#7c3aed")}
        >
          Upload Packages
        </button>

      </div>

      {/* MAIN */}

      <div
        style={{
          flex: 1
        }}
      >

        {
          screen === "graph"

            ?

            <GraphView
              nodes={[]}
              edges={[]}
              onNodeClick={() => {}}
            />

            :

            <OraclePackageUploader />
        }

      </div>

    </div>
  );
}

function buttonStyle(background) {

  return {

    padding: "14px",

    borderRadius: "12px",

    border: "none",

    background,

    color: "white",

    cursor: "pointer",

    fontWeight: "bold"
  };
}

export default App;