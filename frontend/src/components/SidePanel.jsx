export default function SidePanel({ node }) {

  if (!node) {

    return (

      <div
        style={{
          position: "absolute",
          right: 0,
          top: 0,
          width: "250px",
          height: "100%",
          background: "#111827",
          color: "white",
          padding: "20px"
        }}
      >

        <h2>LegacyMind</h2>

        <p>Seleccioná un nodo</p>

      </div>
    );
  }

  return (

    <div
      style={{
        position: "absolute",
        right: 0,
        top: 0,
        width: "250px",
        height: "100%",
        background: "#111827",
        color: "white",
        padding: "20px"
      }}
    >

      <h2>{node.data.label}</h2>

      <hr />

      <p>

        <strong>Tipo:</strong>

        {" "}

        {node.data.nodeType}

      </p>

    </div>
  );
}