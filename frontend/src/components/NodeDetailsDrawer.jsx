function Section({ title, children }) {
  return (
    <section>
      <h3 style={{ color: "#38bdf8", fontSize: "15px", marginBottom: "8px" }}>{title}</h3>
      {children}
    </section>
  );
}

function ItemList({ items, emptyMessage }) {
  if (!items?.length) return <p style={{ color: "#64748b", fontSize: "13px" }}>{emptyMessage}</p>;
  return (
    <ul style={{ margin: 0, paddingLeft: "20px", color: "#cbd5e1", fontSize: "13px" }}>
      {items.map((item, index) => <li key={`${item}-${index}`}>{item}</li>)}
    </ul>
  );
}

export default function NodeDetailsDrawer({ node, onClose }) {
  if (!node) return null;

  const data = node.data || node;
  const type = (data.type || data.nodeType || "UNKNOWN").toUpperCase();
  const isTable = type === "TABLE";
  const isExecutable = ["PROCEDURE", "FUNCTION"].includes(type);

  return (
    <aside style={{
      width: "380px",
      flexShrink: 0,
      background: "#111827",
      borderLeft: "1px solid #1f2937",
      padding: "22px",
      overflowY: "auto",
      display: "flex",
      flexDirection: "column",
      gap: "20px"
    }}>
      <div style={{ display: "flex", justifyContent: "space-between", gap: "12px" }}>
        <div>
          <h2 style={{ margin: 0, fontSize: "20px", wordBreak: "break-word" }}>{data.name || data.label}</h2>
          <span style={{ color: "#94a3b8", fontSize: "13px" }}>{type}</span>
        </div>
        <button aria-label="Close node details" onClick={onClose} style={{
          alignSelf: "flex-start",
          background: "#dc2626",
          border: 0,
          color: "white",
          borderRadius: "8px",
          padding: "8px 12px",
          cursor: "pointer"
        }}>✕</button>
      </div>

      {isTable && (
        <Section title="Packages and procedures accessing this table">
          {data.accessedBy?.length ? data.accessedBy.map((accessor, index) => (
            <div key={`${accessor.name}-${accessor.relation}-${index}`} style={{
              background: "#0f172a", border: "1px solid #1f2937", borderRadius: "8px",
              padding: "9px", marginBottom: "7px", fontSize: "13px"
            }}>
              <strong>{accessor.name}</strong>
              <span style={{ color: accessor.relation === "WRITES" ? "#f97316" : "#38bdf8", marginLeft: "8px" }}>
                {accessor.relation}
              </span>
            </div>
          )) : <p style={{ color: "#64748b", fontSize: "13px" }}>No READ/WRITE relations found.</p>}
        </Section>
      )}

      {isExecutable && (
        <>
          <Section title="Tables read">
            <ItemList items={data.reads} emptyMessage="No table reads found." />
          </Section>
          <Section title="Tables written">
            <ItemList items={data.writes} emptyMessage="No table writes found." />
          </Section>
          <Section title="AST code snippets">
            {data.codeSnippets?.length ? data.codeSnippets.map((snippet, index) => (
              <pre key={index} style={{
                whiteSpace: "pre-wrap", overflowWrap: "anywhere", background: "#020617",
                color: "#a7f3d0", borderRadius: "8px", padding: "12px", fontSize: "12px"
              }}>{snippet}</pre>
            )) : <p style={{ color: "#64748b", fontSize: "13px" }}>No source evidence available.</p>}
          </Section>
        </>
      )}

      {type === "PACKAGE" && (
        <Section title="Subprograms">
          <ItemList
            items={data.subprograms?.map(({ qualifiedName, name, type: subprogramType }) =>
              `${qualifiedName || name} (${subprogramType || "SUBPROGRAM"})`)}
            emptyMessage="No associated subprograms found."
          />
        </Section>
      )}
    </aside>
  );
}
