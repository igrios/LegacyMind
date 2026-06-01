


export default function SidePanel({ node }) {

  if (!node) {

    return (

      <div
        style={{
          position: "absolute",
          right: 0,
          top: 0,
          width: "320px",
          height: "100%",
          background: "#111827",
          color: "white",
          padding: "20px",
          overflowY: "auto"
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
        width: "320px",
        height: "100%",
        background: "#111827",
        color: "white",
        padding: "20px",
        overflowY: "auto"
      }}
    >

      <h2>{node.name}</h2>

      <hr />

      <p>
        <strong>Type:</strong> {node.type}
      </p>

      <p>
        <strong>Risk:</strong> {node.riskLevel}
      </p>

      <p>
        <strong>Score:</strong> {node.riskScore}
      </p>

      {node.businessRules?.length > 0 && (

        <>
          <hr />

          <h3>Business Rules</h3>

          {node.businessRules.map(rule => (

            <div key={rule.errorCode}>

              <strong>{rule.errorCode}</strong>

              <p>{rule.message}</p>

            </div>

          ))}
        </>

      )}

      {node.referencedTables?.length > 0 && (

        <>
          <hr />

          <h3>Tables</h3>

          <ul>

            {node.referencedTables.map(table => (

              <li key={table}>
                {table}
              </li>

            ))}

          </ul>

        </>

      )}

      {node.codeSmells?.length > 0 && (

        <>
          <hr />

          <h3>Code Smells</h3>

          <ul>

            {node.codeSmells.map(smell => (

              <li key={smell}>
                {smell}
              </li>

            ))}

          </ul>

        </>

      )}

    </div>
  );
}