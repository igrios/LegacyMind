package com.ignacio.legacyanalyzer.infrastructure.adapters.output.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import com.ignacio.legacyanalyzer.domain.model.LegacyObject;

class RegexLegacyParserAdapterTest {

    private final RegexLegacyParserAdapter parser = new RegexLegacyParserAdapter();

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

        assertEquals("TEST_PROC", result.getName());
        assertEquals("PROCEDURE", result.getType());
        assertTrue(result.getReferencedTables().contains("CUSTOMERS"));
        assertTrue(result.getReferencedTables().contains("ORDERS"));
    }

    @Test
    void shouldCalculateHighRiskWhenMultipleSmellsDetected() {

        String sql = """
            CREATE OR REPLACE PROCEDURE risk_proc IS
            BEGIN
                SELECT * FROM users;
                EXECUTE IMMEDIATE 'DELETE FROM logs';
                COMMIT;
            EXCEPTION
                WHEN OTHERS THEN
                    NULL;
            END;
        """;

        LegacyObject result = parser.parse(sql);

        assertEquals("RISK_PROC", result.getName());
        assertEquals("PROCEDURE", result.getType());
        assertEquals("HIGH", result.getRiskLevel());
        assertTrue(result.getRiskScore() >= 7);
    }

    @Test
    void shouldHandleCleanFunction() {

        String sql = """
            CREATE OR REPLACE FUNCTION clean_func RETURN NUMBER IS
            BEGIN
                RETURN 1;
            END;
        """;

        LegacyObject result = parser.parse(sql);

        assertEquals("CLEAN_FUNC", result.getName());
        assertEquals("FUNCTION", result.getType());
        assertEquals("LOW", result.getRiskLevel());
        assertTrue(result.getCodeSmells().isEmpty());
    }
}