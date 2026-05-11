
import React from "react";

export default function OraclePackageUploader() {
  const [sourceCode, setSourceCode] = React.useState("");
  const [loading, setLoading] = React.useState(false);
  const [response, setResponse] = React.useState(null);
  const [error, setError] = React.useState(null);

  const API_URL = process.env.REACT_APP_API_URL;

  const handleFileUpload = async (event) => {
    const file = event.target.files?.[0];

    if (!file) return;

    try {
      const text = await file.text();
      setSourceCode(text);
    } catch (err) {
      console.error(err);
      setError("No se pudo leer el archivo.");
    }
  };

  const buildPayload = () => {
    return {
      sourceCode,
    };
  };

  const handleAnalyze = async () => {
    setLoading(true);
    setError(null);
    setResponse(null);

    try {
      const payload = buildPayload();

      const res = await fetch(`${API_URL}/analyze`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(payload),
      });

      if (!res.ok) {
        throw new Error(`HTTP ${res.status}`);
      }

      const data = await res.json();
      setResponse(data);
    } catch (err) {
      console.error(err);
      setError("Error analizando paquete PL/SQL");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-100 p-6">
      <div className="max-w-6xl mx-auto bg-white rounded-2xl shadow-xl p-6">
        <div className="mb-6">
          <h1 className="text-3xl font-bold mb-2">LegacyMind</h1>
          <p className="text-gray-600">
            Subí paquetes Oracle PL/SQL o pegá código manualmente para analizar dependencias y relaciones.
          </p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <div className="space-y-4">
            <div className="border-2 border-dashed border-gray-300 rounded-2xl p-6 text-center bg-gray-50">
              <input
                type="file"
                accept=".sql,.pck,.pkb,.pkg,.txt"
                onChange={handleFileUpload}
                className="mb-4"
              />

              <p className="text-sm text-gray-500">
                Formatos soportados: .sql .pck .pkb .pkg
              </p>
            </div>

            <div>
              <label className="block font-semibold mb-2">
                Código fuente Oracle
              </label>

              <textarea
                value={sourceCode}
                onChange={(e) => setSourceCode(e.target.value)}
                placeholder="Pegá acá packages Oracle PL/SQL..."
                className="w-full h-[500px] p-4 border rounded-2xl font-mono text-sm bg-black text-green-400"
              />
            </div>
          </div>

          <div className="space-y-4">
            <div className="bg-gray-900 text-green-400 rounded-2xl p-4 h-[300px] overflow-auto">
              <h2 className="font-bold mb-2 text-white">Payload generado</h2>

              <pre className="text-xs whitespace-pre-wrap break-words">
                {JSON.stringify(buildPayload(), null, 2)}
              </pre>
            </div>

            <button
              onClick={handleAnalyze}
              disabled={loading || !sourceCode}
              className="w-full bg-black text-white rounded-2xl py-4 font-semibold hover:opacity-90 disabled:opacity-50"
            >
              {loading ? "Analizando..." : "Analizar paquete"}
            </button>

            {error && (
              <div className="bg-red-100 border border-red-300 text-red-700 rounded-2xl p-4">
                {error}
              </div>
            )}

            {response && (
              <div className="bg-gray-900 text-green-400 rounded-2xl p-4 h-[400px] overflow-auto">
                <h2 className="font-bold mb-2 text-white">Respuesta API</h2>

                <pre className="text-xs whitespace-pre-wrap break-words">
                  {JSON.stringify(response, null, 2)}
                </pre>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
