import { useState } from "react";

export default function OraclePackageUploader({
  onAnalyze
}) {

  // =====================================================
  // SOURCE CODE
  // =====================================================

  const [sourceCode, setSourceCode] = useState(`CREATE OR REPLACE PROCEDURE TEST IS
BEGIN
    NULL;
END;`);

  const [analysisResult, setAnalysisResult] = useState("");

  // =====================================================
  // ANALYZE
  // =====================================================

  const handleSubmit = async () => {

    try {

      console.log("ANALYZE CLICK");

      console.log("SOURCE CODE:", sourceCode);

      if (!onAnalyze) {

        console.error("onAnalyze undefined");

        return;
      }

      await onAnalyze(sourceCode);

      setAnalysisResult(`
✔ Semantic analysis completed

✔ Dependency graph generated

✔ Oracle PL/SQL parsed successfully

✔ Knowledge graph updated
      `);

    } catch (error) {

      console.error(error);

      setAnalysisResult(`
✖ Error analyzing package

Check backend logs or API response.
      `);
    }
  };

  // =====================================================
  // COMPONENT
  // =====================================================

  return (

    <div
      style={{
        display: "flex",
        flexDirection: "column",
        gap: "20px",
        height: "100%",
        padding: "30px",
        background: "#0f172a",
        overflow: "auto"
      }}
    >

      {/* ============================================= */}
      {/* TITLE */}
      {/* ============================================= */}

      <div>

        <h2
          style={{
            color: "white",
            marginBottom: "8px",
            fontSize: "32px"
          }}
        >
          Upload Oracle PL/SQL
        </h2>

        <p
          style={{
            color: "#94a3b8"
          }}
        >
          Paste procedures, packages, triggers or views for semantic analysis.
        </p>

      </div>

      {/* ============================================= */}
      {/* TEXTAREA */}
      {/* ============================================= */}

      <textarea
        value={sourceCode}
        onChange={(e) =>
          setSourceCode(e.target.value)
        }
        placeholder="Paste Oracle PL/SQL here..."
        style={{
          width: "100%",
          height: "220px",
          background: "#020617",
          color: "#e2e8f0",
          border: "1px solid #334155",
          borderRadius: "16px",
          padding: "18px",
          fontSize: "14px",
          fontFamily: "monospace",
          resize: "vertical",
          outline: "none",
          boxSizing: "border-box",
          lineHeight: "1.6"
        }}
      />

      {/* ============================================= */}
      {/* BUTTON */}
      {/* ============================================= */}

      <button
        onClick={handleSubmit}
        style={{
          padding: "16px",
          background: "#2563eb",
          border: "none",
          borderRadius: "14px",
          color: "white",
          fontWeight: "bold",
          fontSize: "16px",
          cursor: "pointer",
          transition: "all 0.2s ease"
        }}
      >
        Analyze Package
      </button>

      {/* ============================================= */}
      {/* ANALYSIS RESULT */}
      {/* ============================================= */}

      <div
        style={{
          background: "#111827",
          border: "1px solid #1f2937",
          borderRadius: "16px",
          padding: "20px",
          minHeight: "180px"
        }}
      >

        <h3
          style={{
            marginBottom: "16px",
            color: "white",
            fontSize: "20px"
          }}
        >
          Analysis Output
        </h3>

        <pre
          style={{
            color: "#93c5fd",
            whiteSpace: "pre-wrap",
            fontFamily: "monospace",
            fontSize: "14px",
            lineHeight: "1.6"
          }}
        >
          {analysisResult || `
Waiting for analysis...

• Dependency extraction
• Semantic graph generation
• PL/SQL parsing
• Risk detection
• Knowledge graph update
          `}
        </pre>

      </div>

    </div>
  );
}