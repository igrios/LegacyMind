package com.ignacio.legacyanalyzer.infrastructure.adapters.output.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.ignacio.legacyanalyzer.domain.model.JoinCondition;
import com.ignacio.legacyanalyzer.domain.model.LegacyObject;
import com.ignacio.legacyanalyzer.domain.model.SqlSemanticModel;
import com.ignacio.legacyanalyzer.domain.model.TableReference;

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

        assertNotNull(result);
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

        assertNotNull(result);
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

        assertNotNull(result);
        assertEquals("CLEAN_FUNC", result.getName());
        assertEquals("FUNCTION", result.getType());

        assertEquals("LOW", result.getRiskLevel());
        assertTrue(result.getCodeSmells().isEmpty());
    }

    @Test
    void shouldExtractSemanticRelationsFromInsertSelect() {

        String sql = """
                    INSERT INTO TBL_FACTURAS
                    SELECT * FROM TBL_PEDIDOS;
                """;

        var relations = parser.extractSemanticRelations(sql);

        assertNotNull(relations);
        assertTrue(relations.contains("TBL_PEDIDOS->TBL_FACTURAS"));
    }

    @Test
    void shouldExtractMultipleSourcesFromInsert() {

        String sql = """
                    INSERT INTO TBL_A
                    SELECT * FROM TBL_B, TBL_C;
                """;

        var relations = parser.extractSemanticRelations(sql);

        assertTrue(relations.contains("TBL_B->TBL_A"));
        assertTrue(relations.contains("TBL_C->TBL_A"));


    }

    @Test
    void shouldExtractSchemaQualifiedTables() {

        String sql = """
                CREATE OR REPLACE PROCEDURE TEST_PROC IS
                BEGIN

                    SELECT *
                    FROM CRM.CLIENTES C,
                         ERP.PEDIDOS P

                    WHERE C.ID = P.CLIENTE_ID;

                END;
                """;

        LegacyObject object = parser.parse(sql);

        System.out.println(object.getReferencedTables());

        assertTrue(object.getReferencedTables().contains("CRM.CLIENTES"));

        assertTrue(object.getReferencedTables().contains("ERP.PEDIDOS"));
    }


    @Test
    void shouldExtractExplicitJoinTables() {

        String sql = """
                CREATE OR REPLACE PROCEDURE TEST_PROC IS
                BEGIN

                    SELECT *
                    FROM CRM.CLIENTES C
                    INNER JOIN ERP.PEDIDOS P
                        ON C.ID = P.CLIENTE_ID;

                END;
                """;

        LegacyObject object = parser.parse(sql);

        System.out.println(object.getReferencedTables());

        assertTrue(object.getReferencedTables().contains("CRM.CLIENTES"));

        assertTrue(object.getReferencedTables().contains("ERP.PEDIDOS"));
    }

    @Test
    void shouldExtractTableAliases() {

        String sql = """
                CREATE OR REPLACE PROCEDURE TEST_PROC IS
                BEGIN

                    SELECT *
                    FROM CRM.CLIENTES C
                    INNER JOIN ERP.PEDIDOS P
                        ON C.ID = P.CLIENTE_ID;

                END;
                """;

        String fromClause = parser.extractTopLevelFromClause(sql.toUpperCase());

        List<TableReference> refs = parser.extractTableReferences(fromClause);

        System.out.println(refs);

        assertEquals("C", refs.get(0).getAlias());

        assertEquals("CRM.CLIENTES", refs.get(0).getFullName());
    }

    @Test
    void shouldExtractJoinConditions() {

        String sql = """
                SELECT *
                FROM CRM.CLIENTES C
                INNER JOIN ERP.PEDIDOS P
                    ON C.ID = P.CLIENTE_ID
                WHERE C.STATUS = P.STATUS;
                """;

        List<JoinCondition> conditions = parser.extractJoinConditions(sql);

        System.out.println(conditions);

        assertEquals(2, conditions.size());

        assertEquals("C", conditions.get(0).getLeftAlias());

        assertEquals("ID", conditions.get(0).getLeftColumn());

        assertEquals("P", conditions.get(0).getRightAlias());

        assertEquals("CLIENTE_ID", conditions.get(0).getRightColumn());
    }

    @Test
void shouldResolveJoinAliases() {

    String sql = """
        SELECT *
        FROM CRM.CLIENTES C
        INNER JOIN ERP.PEDIDOS P
            ON C.ID = P.CLIENTE_ID
        WHERE C.STATUS = P.STATUS;
        """;

    String fromClause =
            parser.extractTopLevelFromClause(sql.toUpperCase());

    List<TableReference> refs =
            parser.extractTableReferences(fromClause);

    List<JoinCondition> conditions =
            parser.extractJoinConditions(sql);

    parser.resolveJoinConditions(refs, conditions);
}

@Test
void shouldBuildSemanticModel() {

    String sql = """
        SELECT *
        FROM CRM.CLIENTES C
        INNER JOIN ERP.PEDIDOS P
            ON C.ID = P.CLIENTE_ID;
        """;

    SqlSemanticModel model =
            parser.buildSemanticModel(sql);

    System.out.println(model);

    assertFalse(model.getReadTables().isEmpty());

    assertFalse(model.getTableReferences().isEmpty());

    assertFalse(model.getJoinConditions().isEmpty());
}

}
