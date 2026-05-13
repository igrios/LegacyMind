package com.ignacio.legacyanalyzer.infrastructure.adapters.output.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import com.ignacio.legacyanalyzer.domain.model.LegacyObject;

public class LegacyImplicitJoinCasesTest {

  private final RegexLegacyParserAdapter parser = new RegexLegacyParserAdapter();

  @Test
  void  shouldDetectImplicitJoinTables() { 
    
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





}
