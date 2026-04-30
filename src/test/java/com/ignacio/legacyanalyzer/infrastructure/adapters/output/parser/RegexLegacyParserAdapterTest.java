package com.ignacio.legacyanalyzer.infrastructure.adapters.output.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ignacio.legacyanalyzer.domain.model.LegacyObject;
import java.util.List;
import org.junit.jupiter.api.Test;

class RegexLegacyParserAdapterTest {

    private final RegexLegacyParserAdapter parser = new RegexLegacyParserAdapter();

    @Test
    void parseShouldExtractObjectMetadataAndRiskWhenSmellsDetected() {
        String sourceCode = """
                CREATE OR REPLACE PROCEDURE process_orders IS
                BEGIN
                    SELECT * FROM customers c
                    JOIN orders o ON c.id = o.customer_id;
                    EXECUTE IMMEDIATE 'DELETE FROM logs';
                    COMMIT;
                EXCEPTION
                    WHEN OTHERS THEN
                        NULL;
                END;
                """;

        LegacyObject result = parser.parse(sourceCode);

        assertEquals("PROCESS_ORDERS", result.getName());
        assertEquals("PROCEDURE", result.getType());
        assertTrue(result.getReferencedTables().contains("CUSTOMERS"));
        assertTrue(result.getReferencedTables().contains("ORDERS"));
        assertTrue(result.getCodeSmells().contains("SELECT * detected"));
        assertTrue(result.getCodeSmells().contains("COMMIT inside procedure"));
        assertTrue(result.getCodeSmells().contains("WHEN OTHERS generic exception handling"));
        assertTrue(result.getCodeSmells().contains("Dynamic SQL detected (EXECUTE IMMEDIATE)"));
        assertEquals(11, result.getRiskScore());
        assertEquals("HIGH", result.getRiskLevel());
    }

    @Test
    void parseShouldReturnLowRiskForCleanFunction() {
        String sourceCode = """
                CREATE OR REPLACE FUNCTION calc_total RETURN NUMBER IS
                BEGIN
                    RETURN 1;
                END;
                """;

        LegacyObject result = parser.parse(sourceCode);

        assertEquals("CALC_TOTAL", result.getName());
        assertEquals("FUNCTION", result.getType());
        assertEquals(List.of(), result.getCodeSmells());
        assertEquals(0, result.getRiskScore());
        assertEquals("LOW", result.getRiskLevel());
    }
}
