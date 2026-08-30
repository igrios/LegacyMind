package com.ignacio.legacyanalyzer.infrastructure.adapters.output.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import com.ignacio.legacyanalyzer.domain.model.JoinCondition;
import com.ignacio.legacyanalyzer.domain.model.LegacyObject;
import com.ignacio.legacyanalyzer.domain.model.SqlSemanticModel;
import com.ignacio.legacyanalyzer.domain.model.TableReference;
import com.ignacio.legacyanalyzer.domain.services.risk.LegacyRiskAnalyzer;
import com.ignacio.legacyanalyzer.domain.services.semantic.CursorSemanticExtractor;
import com.ignacio.legacyanalyzer.domain.services.semantic.GraphRelationExtractor;
import com.ignacio.legacyanalyzer.domain.services.semantic.SqlSemanticExtractor;

class RegexLegacyParserAdapterTest {



    private final SqlSemanticExtractor semanticExtractor = new SqlSemanticExtractor();

    private final GraphRelationExtractor graphRelationExtractor =
            new GraphRelationExtractor(semanticExtractor);

    private final RegexLegacyParserAdapter parser =
        new RegexLegacyParserAdapter(

                new LegacyRiskAnalyzer(),

                graphRelationExtractor,

                semanticExtractor,

                new CursorSemanticExtractor());

    @Test
    void logisticsPackageShouldOnlyCreateNodesForRealTables() {
        String source = """
                CREATE OR REPLACE PACKAGE BODY PKG_LOGISTICA_Y_FACTURACION_LEGACY AS
                  PROCEDURE PROCESAR(P_ID_CLIENTE NUMBER) IS
                    V_MONTO_FINAL NUMBER;
                    CURSOR C_FACT IS SELECT ID_CLIENTE FROM FACTURAS_PENDIENTES;
                  BEGIN
                    SELECT NIVEL INTO V_MONTO_FINAL FROM CLIENTES_VIP
                     WHERE ID_CLIENTE = P_ID_CLIENTE;
                    SELECT SYSDATE, USER INTO V_MONTO_FINAL, V_MONTO_FINAL FROM DUAL;
                    UPDATE INVENTARIO_DEPOSITO SET STOCK = STOCK - 1;
                    INSERT INTO DESPACHOS_CONFIRMADOS (ID_CLIENTE, FECHA)
                    VALUES (P_ID_CLIENTE, SYSDATE);
                    SELECT ESTADO INTO V_MONTO_FINAL FROM PEDIDOS_CABECERA;
                    INSERT INTO LOG_INCIDENCIAS_LOGISTICA (CODIGO) VALUES (SQLCODE);
                    INSERT INTO FACTURAS_EMITIDAS (ID_CLIENTE) VALUES (P_ID_CLIENTE);
                    UPDATE CUENTAS_CORRIENTES_CLIENTES SET SALDO = V_MONTO_FINAL;
                    INSERT INTO AUDITORIA_TRANSACCIONAL (USUARIO) VALUES (USER);

                    FOR R_FACT IN C_FACT LOOP
                      V_MONTO_FINAL := R_FACT.ID_CLIENTE;
                    END LOOP;

                    OPEN C_FACT FOR 'SELECT ID_CLIENTE FROM TABLA_FANTASMA'
                      USING V_MONTO_FINAL;
                    -- UPDATE V_VARIABLE_FANTASMA SET VALOR = 1;
                  END PROCESAR;
                END PKG_LOGISTICA_Y_FACTURACION_LEGACY;
                """;

        LegacyObject result = parser.parse(source);

        assertEquals(Set.of(
                "CLIENTES_VIP",
                "FACTURAS_PENDIENTES",
                "INVENTARIO_DEPOSITO",
                "DESPACHOS_CONFIRMADOS",
                "PEDIDOS_CABECERA",
                "LOG_INCIDENCIAS_LOGISTICA",
                "FACTURAS_EMITIDAS",
                "CUENTAS_CORRIENTES_CLIENTES",
                "AUDITORIA_TRANSACCIONAL"), Set.copyOf(result.getReferencedTables()));
    }

    @Test
    void shouldRejectVariablesRecordFieldsAndOraclePseudoColumnsAsTables() {
        List<String> falseCandidates = List.of(
                "SYSDATE", "USER", "DUAL", "NEXTVAL", "CURRVAL", "SQLERRM", "SQLCODE",
                "ROWNUM", "ID_DESPACHO", "FECHA_FACTURA", "USUARIO", "FECHA",
                "R_FACT.ID_CLIENTE", "V_MONTO_FINAL", "P_CLIENTE",
                "R_REGISTRO", "C_CURSOR");

        falseCandidates.forEach(candidate ->
                assertFalse(semanticExtractor.isValidTable(candidate), candidate));
        assertTrue(semanticExtractor.isValidTable("CRM.CLIENTES"));
    }

    @Test
    void selectProjectionAndInSubqueryColumnsMustNeverBecomeTables() {
        String source = """
                CREATE OR REPLACE PROCEDURE SP_CONFIRMAR_DESPACHOS IS
                BEGIN
                  SELECT ID_DESPACHO, FECHA_FACTURA, USUARIO, FECHA
                    FROM DESPACHOS_CONFIRMADOS D
                   WHERE D.ID_DESPACHO IN (
                     SELECT F.ID_DESPACHO
                       FROM FACTURAS_EMITIDAS F
                       JOIN AUDITORIA_TRANSACCIONAL A
                         ON A.ID_DESPACHO = F.ID_DESPACHO
                   );

                  UPDATE PEDIDOS_CABECERA
                     SET FECHA = SYSDATE
                   WHERE ID_DESPACHO IN (
                     SELECT ID_DESPACHO FROM INVENTARIO_DEPOSITO
                   );
                END SP_CONFIRMAR_DESPACHOS;
                """;

        LegacyObject result = parser.parse(source);

        assertEquals(Set.of(
                "DESPACHOS_CONFIRMADOS",
                "FACTURAS_EMITIDAS",
                "AUDITORIA_TRANSACCIONAL",
                "PEDIDOS_CABECERA",
                "INVENTARIO_DEPOSITO"), Set.copyOf(result.getReferencedTables()));
        assertFalse(result.getReferencedTables().contains("ID_DESPACHO"));
        assertFalse(result.getReferencedTables().contains("FECHA_FACTURA"));
        assertFalse(result.getReferencedTables().contains("USUARIO"));
        assertFalse(result.getReferencedTables().contains("FECHA"));
    }
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

        assertEquals("CRITICAL", result.getRiskLevel());

        assertTrue(result.getRiskScore() >= 15);
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

        String fromClause = parser.extractTopLevelFromClause(sql.toUpperCase());

        List<TableReference> refs = parser.extractTableReferences(fromClause);

        List<JoinCondition> conditions = parser.extractJoinConditions(sql);

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

        SqlSemanticModel model = parser.buildSemanticModel(sql);

        System.out.println(model);

        assertFalse(model.getReadTables().isEmpty());

        assertFalse(model.getTableReferences().isEmpty());

        assertFalse(model.getJoinConditions().isEmpty());
    }

    @Test
    void shouldDetectCartesianJoin() {

        String sql = """
                SELECT *
                FROM CLIENTES C,
                     PEDIDOS P;
                """;

        SqlSemanticModel model = parser.buildSemanticModel(sql);

        System.out.println(model);

        assertTrue(
                model.getFindings().stream().anyMatch(f -> f.getType().equals("CARTESIAN_JOIN")));
    }

    @Test
    void shouldNotDetectCartesianJoin() {

        String sql = """
                SELECT *
                FROM CLIENTES C
                INNER JOIN PEDIDOS P
                    ON C.ID = P.CLIENTE_ID;
                """;

        SqlSemanticModel model = parser.buildSemanticModel(sql);

        assertFalse(
                model.getFindings().stream().anyMatch(f -> f.getType().equals("CARTESIAN_JOIN")));
    }


    @Test
    void shouldDetectSelectStarRisk() {

        String sql = """
                SELECT *
                FROM CLIENTES;
                """;

        SqlSemanticModel model = parser.buildSemanticModel(sql);

        System.out.println(model);

        assertTrue(model.getFindings().stream().anyMatch(f -> f.getType().equals("SELECT_STAR")));
    }

    @Test
    void shouldDetectMaterializedView() {

        String sql = """
                CREATE OR REPLACE MATERIALIZED VIEW MV_CLIENTES
                AS
                SELECT *
                FROM CLIENTES;
                """;

        LegacyObject result = parser.parse(sql);

        assertEquals("MV_CLIENTES", result.getName());

        assertEquals("MATERIALIZED_VIEW", result.getType());
    }

    @Test
    void shouldDetectTriggerRelations() {

        String sql = """
                CREATE OR REPLACE TRIGGER TRG_AUDITORIA
                AFTER INSERT ON PAGOS
                FOR EACH ROW
                BEGIN
                    INSERT INTO AUDITORIA_LOG
                    VALUES (:NEW.ID);
                END;
                """;

        LegacyObject result = parser.parse(sql);

        assertEquals("TRIGGER", result.getType());

        boolean found = result.getKnowledgeRelations().stream().anyMatch(r ->

        r.relation().equals("TRIGGER_ON")

                &&

                r.target().equals("PAGOS"));

        assertTrue(found);
    }


    @Test
    void shouldIgnoreAliasColumnsAsTables() {

        String sql = """
                SELECT
                    C.ID_CLIENTE,
                    P.CLIENTE_ID
                FROM CRM.CLIENTES C
                INNER JOIN ERP.PEDIDOS P
                    ON C.ID_CLIENTE = P.CLIENTE_ID
                """;

        List<String> tables = semanticExtractor.extractReadTables(sql);

        assertTrue(tables.contains("CRM.CLIENTES"));

        assertTrue(tables.contains("ERP.PEDIDOS"));

        assertFalse(tables.contains("C.ID_CLIENTE"));

        assertFalse(tables.contains("P.CLIENTE_ID"));
    }

}
