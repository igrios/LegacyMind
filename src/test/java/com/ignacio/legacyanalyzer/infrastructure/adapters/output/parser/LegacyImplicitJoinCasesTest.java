package com.ignacio.legacyanalyzer.infrastructure.adapters.output.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.ignacio.legacyanalyzer.domain.model.KnowledgeRelation;
import com.ignacio.legacyanalyzer.domain.model.LegacyObject;
import com.ignacio.legacyanalyzer.domain.services.risk.LegacyRiskAnalyzer;
import com.ignacio.legacyanalyzer.domain.services.semantic.CursorSemanticExtractor;
import com.ignacio.legacyanalyzer.domain.services.semantic.GraphRelationExtractor;
import com.ignacio.legacyanalyzer.domain.services.semantic.SqlSemanticExtractor;

public class LegacyImplicitJoinCasesTest {

  private final SqlSemanticExtractor semanticExtractor = new SqlSemanticExtractor();

  private final GraphRelationExtractor graphRelationExtractor = new GraphRelationExtractor(
      semanticExtractor);

private final RegexLegacyParserAdapter parser =
        new RegexLegacyParserAdapter(

                new LegacyRiskAnalyzer(),

                graphRelationExtractor,

                semanticExtractor,

                new CursorSemanticExtractor());
  @Test
  void shouldDetectImplicitJoinTables() {
    String sql = """
        CREATE OR REPLACE PROCEDURE test_proc IS
        BEGIN
            SELECT *
            FROM customers c, orders o
            WHERE c.id = o.customer_id;
        END;
        """;

    LegacyObject result = parser.parse(sql);

    assertNotNull(result);
    assertEquals("TEST_PROC", result.getName());
    assertEquals("PROCEDURE", result.getType());

    assertTrue(result.getReferencedTables().contains("CUSTOMERS"));
    assertTrue(result.getReferencedTables().contains("ORDERS"));
  }

  @Test
  void shouldParseThreeTableImplicitJoinChain() {
    String sql = """
        CREATE OR REPLACE PROCEDURE case_two IS
        BEGIN
            SELECT *
            FROM a,
                 b,
                 c
            WHERE a.id = b.id
              AND b.id = c.id;
        END;
        """;

    LegacyObject result = parser.parse(sql);

    assertNotNull(result);
    assertTrue(result.getReferencedTables().contains("A"));
    assertTrue(result.getReferencedTables().contains("B"));
    assertTrue(result.getReferencedTables().contains("C"));
  }

  @Test
  void shouldParseImplicitJoinInsideSubquery() {
    String sql = """
        CREATE OR REPLACE PROCEDURE case_three IS
        BEGIN
            SELECT *
            FROM customers c,
                 (
                   SELECT *
                   FROM orders o,
                        invoices i
                   WHERE o.id = i.order_id
                 ) sub;
        END;
        """;

    LegacyObject result = parser.parse(sql);

    assertNotNull(result);
    assertTrue(result.getReferencedTables().contains("CUSTOMERS"));
    assertTrue(result.getReferencedTables().contains("ORDERS"));
    assertTrue(result.getReferencedTables().contains("INVOICES"));
  }

  @Test
  void shouldParseUpdateWithExistsSubquery() {
    String sql = """
        CREATE OR REPLACE PROCEDURE case_four IS
        BEGIN
            UPDATE customers c
            SET status = 'ACTIVE'
            WHERE EXISTS (
                SELECT 1
                FROM orders o
                WHERE o.customer_id = c.id
            );
        END;
        """;

    LegacyObject result = parser.parse(sql);

    assertNotNull(result);
    assertTrue(result.getReferencedTables().contains("CUSTOMERS"));
    assertTrue(result.getReferencedTables().contains("ORDERS"));
  }

  @Test
  void shouldNotCreateGhostReadNodesForLegacyOuterJoin() {
    String sql = """
        CREATE OR REPLACE PROCEDURE SP_REPORTE_PRODUCTOS IS
        BEGIN
            SELECT p.descripcion, c.CATEGORIA_NOM, s.cantidad
            FROM PRODUCTOS p, CATEGORIAS c, STOCK_DEPOSITO s
            WHERE p.categoria_id = c.id
              AND p.id = s.producto_id(+);
        END;
        """;

    LegacyObject result = parser.parse(sql);

    List<String> readTargets = result.getKnowledgeRelations().stream()
        .filter(r -> "READS".equals(r.relation()))
        .map(KnowledgeRelation::target)
        .toList();

    assertTrue(readTargets.contains("PRODUCTOS"));
    assertTrue(readTargets.contains("CATEGORIAS"));
    assertTrue(readTargets.contains("STOCK_DEPOSITO"));
    assertFalse(readTargets.contains("S"));
    assertFalse(readTargets.contains("EN"));
    assertFalse(readTargets.contains("END"));
  }
}
