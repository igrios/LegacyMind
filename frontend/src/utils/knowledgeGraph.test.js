import { buildKnowledgeGraph } from "./knowledgeGraph";

test("builds a package, subprogram and table hierarchy with source evidence", () => {
  const graph = buildKnowledgeGraph({
    name: "PKG_BANKING",
    type: "PACKAGE BODY",
    subprograms: [{
      name: "TRANSFER",
      qualifiedName: "PKG_BANKING.TRANSFER",
      type: "PROCEDURE",
      body: "UPDATE ACCOUNTS SET BALANCE = BALANCE - P_AMOUNT;",
      reads: ["CUSTOMERS"],
      writes: ["ACCOUNTS"],
      calls: []
    }],
    knowledgeRelations: [{
      source: "PKG_BANKING",
      sourceObject: "PKG_BANKING",
      relation: "WRITES",
      target: "ACCOUNTS",
      codeSnippet: "UPDATE ACCOUNTS SET BALANCE = BALANCE - P_AMOUNT;"
    }]
  });

  expect(graph.nodes.map(({ id }) => id)).toEqual(expect.arrayContaining([
    "PKG_BANKING", "PKG_BANKING.TRANSFER", "CUSTOMERS", "ACCOUNTS"
  ]));
  expect(graph.edges).toEqual(expect.arrayContaining([
    expect.objectContaining({ source: "PKG_BANKING", target: "PKG_BANKING.TRANSFER", label: "CONTAINS" }),
    expect.objectContaining({ source: "PKG_BANKING.TRANSFER", target: "ACCOUNTS", label: "WRITES" })
  ]));

  const procedure = graph.nodes.find(({ id }) => id === "PKG_BANKING.TRANSFER");
  expect(procedure.data.type).toBe("PROCEDURE");
  expect(procedure.data.reads).toEqual(["CUSTOMERS"]);
  expect(procedure.data.writes).toEqual(["ACCOUNTS"]);
  expect(procedure.data.bodySnippet).toContain("UPDATE ACCOUNTS");
  expect(procedure.position.y).toBeGreaterThan(
    graph.nodes.find(({ id }) => id === "PKG_BANKING").position.y
  );
});
