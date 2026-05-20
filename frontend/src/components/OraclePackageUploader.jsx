import { useState } from "react";

export default function OraclePackageUploader({
  onAnalyze
}) {

  // =====================================================
  // STATES
  // =====================================================

  const [sourceCode, setSourceCode] = useState(`CREATE OR REPLACE PROCEDURE TEST IS
BEGIN
    NULL;
END;`);

  const [analysisResult, setAnalysisResult] = useState("");

  // =====================================================
  // FILE UPLOAD
  // =====================================================

  const handleFileUpload = async (event) => {

    const file = event.target.files[0];

    if (!file) return;

    try {

      const text = await file.text();

      setSourceCode(text);

      setAnalysisResult(`
✔ File loaded successfully

✔ File Name: ${file.name}

✔ Ready for semantic analysis
      `);

      console.log("FILE LOADED:", file.name);

    } catch (error) {

      console.error(error);

      setAnalysisResult(`
✖ Error reading file
      `);
    }
  };

  // =====================================================
  // ANALYZE
  // =====================================================

  const handleSubmit = async () => {

    try {

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

✔ Risk engine executed
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
        background: "#020617",
        overflow: "auto"
      }}
    >

      {/* ============================================= */}
      {/* TITLE */}
      {/* ============================================= */}

      <div>

        <h2
          style={{
            color: "#22c55e",
            marginBottom: "8px",
            fontSize: "34px",
            fontWeight: "bold"
          }}
        >
          LegacyMind Oracle Analyzer
        </h2>

        <p
          style={{
            color: "#4ade80",
            fontSize: "15px"
          }}
        >
          Upload Oracle PL/SQL packages, procedures, triggers or views.
        </p>

      </div>

      {/* ============================================= */}
      {/* FILE UPLOAD */}
      {/* ============================================= */}

      <div
        style={{
          display: "flex",
          flexDirection: "column",
          gap: "10px"
        }}
      >

        <label
          style={{
            color: "#22c55e",
            fontWeight: "bold"
          }}
        >
          Upload Oracle Files
        </label>

        <input
          type="file"
          accept=".sql,.pck,.pkb,.pks,.pls,.txt"
          onChange={handleFileUpload}
          style={{
            background: "#111827",
            color: "#22c55e",
            padding: "14px",
            borderRadius: "14px",
            border: "1px solid #22c55e",
            cursor: "pointer"
          }}
        />

      </div>

      {/* ============================================= */}
      {/* TEXTAREA */}
      {/* ============================================= */}

      <div
        style={{
          display: "flex",
          flexDirection: "column",
          gap: "10px"
        }}
      >

        <label
          style={{
            color: "#22c55e",
            fontWeight: "bold"
          }}
        >
          Oracle Source Code
        </label>

        <textarea
          value={sourceCode}
          onChange={(e) =>
            setSourceCode(e.target.value)
          }
          placeholder="Paste Oracle PL/SQL here..."
          style={{
            width: "100%",
            height: "220px",
            background: "#000000",
            color: "#22c55e",
            border: "1px solid #22c55e",
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

      </div>

      {/* ============================================= */}
      {/* ANALYZE BUTTON */}
      {/* ============================================= */}

      <button
        onClick={handleSubmit}
        style={{
          padding: "16px",
          background: "#16a34a",
          border: "none",
          borderRadius: "14px",
          color: "black",
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
          background: "#000000",
          border: "1px solid #22c55e",
          borderRadius: "16px",
          padding: "20px",
          minHeight: "180px"
        }}
      >

        <h3
          style={{
            marginBottom: "16px",
            color: "#22c55e",
            fontSize: "22px"
          }}
        >
          Analysis Output
        </h3>

        <pre
          style={{
            color: "#22c55e",
            whiteSpace: "pre-wrap",
            fontFamily: "monospace",
            fontSize: "14px",
            lineHeight: "1.6"
          }}
        >
          {analysisResult || `
Waiting for analysis...

✔ Dependency extraction
✔ Semantic graph generation
✔ PL/SQL parsing
✔ Risk detection
✔ Knowledge graph update
          `}
        </pre>

      </div>

    </div>
  );
}