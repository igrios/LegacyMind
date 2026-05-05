export default function SidePanel({ node }) {
  if (!node) return null;

  return (
    <div style={{
      position: "absolute",
      right: 0,
      top: 0,
      width: "260px",
      height: "100%",
      background: "#111",
      color: "#fff",
      padding: "15px"
    }}>
      <h2>{node}</h2>

      <p><b>Tipo:</b> {node.startsWith("PRC_") ? "Procedure" : "Tabla"}</p>

      <p><b>Impacto estimado:</b> Medio</p>
      <p><b>Operaciones:</b> READ / WRITE</p>

      <hr />

      <p style={{ fontSize: "12px", opacity: 0.7 }}>
        Click en nodos para explorar dependencias.
      </p>
    </div>
  );
}