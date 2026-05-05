import { useState } from "react";

export default function ControlPanel({ onAnalyze, onReset }) {
  const [table, setTable] = useState("CUENTAS");

  return (
    <div style={{ padding: 10, background: "#eee" }}>
      <input
        value={table}
        onChange={(e) => setTable(e.target.value)}
      />
      <button onClick={() => onAnalyze(table)}>
        Analizar
      </button>

      <button onClick={onReset} style={{ marginLeft: 10 }}>
        Reset
      </button>
    </div>
  );
}