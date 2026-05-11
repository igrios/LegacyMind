import React from "react";

export default function OraclePackageUploader() {

  const [sourceCode, setSourceCode] = React.useState("");

  const [response, setResponse] = React.useState(null);

  const [loading, setLoading] = React.useState(false);

  const API_URL = process.env.REACT_APP_API_URL;

  // =====================================================
  // FILE UPLOAD
  // =====================================================

  const handleFileUpload = async (event) => {

    const file = event.target.files?.[0];

    if (!file) return;

    const text = await file.text();

    setSourceCode(text);
  };

  // =====================================================
  // ANALYZE
  // =====================================================

  const handleAnalyze = async () => {

    try {

      setLoading(true);

      const res = await fetch(

        `${API_URL}/analyze`,

        {

          method: "POST",

          headers: {

            "Content-Type": "application/json"
          },

          body: JSON.stringify({

            sourceCode
          })
        }
      );

      const data = await res.json();

      setResponse(data);

    } catch (err) {

      console.error(err);

    } finally {

      setLoading(false);
    }
  };

  // =====================================================
  // UI
  // =====================================================

  return (

    <div
      style={{
        padding: "30px",
        height: "100vh",
        overflow: "auto",
        background: "#0f172a",
        color: "white"
      }}
    >

      {/* ========================================= */}
      {/* HEADER */}
      {/* ========================================= */}

      <div
        style={{
          marginBottom: "25px"
        }}
      >

        <h1
          style={{
            fontSize: "42px",
            marginBottom: "10px"
          }}
        >
          Oracle Package Analyzer
        </h1>

        <p
          style={{
            color: "#94a3b8",
            fontSize: "16px"
          }}
        >
          Upload Oracle PL/SQL packages and analyze dependencies,
          procedures, tables and legacy risk.
        </p>

      </div>

      {/* ========================================= */}
      {/* FILE UPLOAD */}
      {/* ========================================= */}

      <div
        style={{
          background: "#111827",
          borderRadius: "18px",
          padding: "25px",
          marginBottom: "25px",
          border: "1px solid #1f2937"
        }}
      >

        <h2
          style={{
            marginBottom: "15px"
          }}
        >
          Upload Package
        </h2>

        <input
          type="file"
          accept=".sql,.pck,.pkb,.pkg,.txt"
          onChange={handleFileUpload}
          style={{
            marginBottom: "15px"
          }}
        />

        <p
          style={{
            color: "#94a3b8"
          }}
        >
          Supported formats:
          {" "}
          .sql .pck .pkb .pkg
        </p>

      </div>

      {/* ========================================= */}
      {/* SOURCE CODE */}
      {/* ========================================= */}

      <div
        style={{
          background: "#111827",
          borderRadius: "18px",
          padding: "25px",
          border: "1px solid #1f2937"
        }}
      >

        <h2
          style={{
            marginBottom: "15px"
          }}
        >
          Oracle Source Code
        </h2>

        <textarea

          value={sourceCode}

          onChange={(e) => setSourceCode(e.target.value)}

          placeholder="Paste Oracle PL/SQL package here..."

          style={{

            width: "100%",

            height: "350px",

            background: "#020617",

            color: "#22c55e",

            border: "1px solid #334155",

            borderRadius: "14px",

            padding: "18px",

            fontFamily: "monospace",

            fontSize: "14px",

            resize: "vertical",

            outline: "none",

            lineHeight: "1.6"
          }}
        />

        <button

          onClick={handleAnalyze}

          disabled={loading || !sourceCode}

          style={{

            marginTop: "20px",

            background: loading

              ? "#475569"

              : "#2563eb",

            color: "white",

            border: "none",

            padding: "14px 24px",

            borderRadius: "12px",

            cursor: "pointer",

            fontWeight: "bold",

            fontSize: "15px"
          }}
        >

          {
            loading

              ?

              "Analyzing..."

              :

              "Analyze Package"
          }

        </button>

      </div>

      {/* ========================================= */}
      {/* PAYLOAD */}
      {/* ========================================= */}

      <div
        style={{
          background: "#111827",
          borderRadius: "18px",
          padding: "25px",
          marginTop: "25px",
          border: "1px solid #1f2937"
        }}
      >

        <h2
          style={{
            marginBottom: "15px"
          }}
        >
          Generated Payload
        </h2>

        <pre
          style={{
            background: "#020617",
            padding: "20px",
            borderRadius: "14px",
            overflow: "auto",
            color: "#38bdf8"
          }}
        >
          {

            JSON.stringify(

              {
                sourceCode
              },

              null,

              2
            )
          }
        </pre>

      </div>

      {/* ========================================= */}
      {/* RESPONSE */}
      {/* ========================================= */}

      {

        response && (

          <div
            style={{
              background: "#111827",
              borderRadius: "18px",
              padding: "25px",
              marginTop: "25px",
              marginBottom: "120px",
              border: "1px solid #1f2937"
            }}
          >

            <h2
              style={{
                marginBottom: "15px"
              }}
            >
              Analysis Result
            </h2>

            <pre
              style={{
                background: "#020617",
                padding: "20px",
                borderRadius: "14px",
                overflow: "auto",
                color: "#22c55e",
                maxHeight: "700px"
              }}
            >
              {
                JSON.stringify(
                  response,
                  null,
                  2
                )
              }
            </pre>

          </div>
        )
      }

    </div>
  );
}