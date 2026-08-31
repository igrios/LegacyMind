package com.ignacio.legacyanalyzer.domain.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.ignacio.legacyanalyzer.domain.model.KnowledgeRelation;
import com.ignacio.legacyanalyzer.domain.services.semantic.GraphRelationExtractor;
import com.ignacio.legacyanalyzer.domain.services.semantic.SqlSemanticExtractor;

class GraphRelationExtractorTest {

    private GraphRelationExtractor graphRelationExtractor;

    @BeforeEach
    void setup() {

        SqlSemanticExtractor semanticExtractor =
                new SqlSemanticExtractor();

        graphRelationExtractor =
                new GraphRelationExtractor(
                        semanticExtractor);
    }

    @Test
    void should_capture_graph_rag_evidence_with_original_oracle_syntax() {
        String sourceCode = """
                CREATE OR REPLACE PROCEDURE SP_REPORTE IS
                BEGIN
                    SELECT p.id
                    FROM PRODUCTOS p, STOCK s
                    WHERE p.id = s.producto_id(+);
                END;
                """;

        KnowledgeRelation relation = graphRelationExtractor.extractKnowledgeRelations(
                        sourceCode, "SP_REPORTE", List.of(), "analysis-evidence")
                .stream()
                .filter(candidate -> candidate.relation().equals("READS"))
                .filter(candidate -> candidate.target().equals("STOCK"))
                .findFirst()
                .orElseThrow();

        assertEquals("SP_REPORTE", relation.sourceObject());
        assertEquals("analysis-evidence", relation.analysisId());
        assertEquals(0.8d, relation.confidenceLevel());
        assertTrue(relation.sourceLineStart() > 0);
        assertTrue(relation.sourceLineEnd() >= relation.sourceLineStart());
        assertTrue(relation.codeSnippet().contains("s.producto_id(+)"));
    }

    @Test
    void should_ignore_oracle_legacy_outer_join_aliases() {

        String sourceCode = """
            CREATE OR REPLACE PROCEDURE SP_REPORTE_PRODUCTOS IS
            BEGIN

                -- Query con Joins implícitos Oracle legacy

                SELECT p.descripcion,
                       c.CATEGORIA_NOM,
                       s.cantidad

                FROM PRODUCTOS p,
                     CATEGORIAS c,
                     STOCK_DEPOSITO s

                WHERE p.categoria_id = c.id
                  AND p.id = s.producto_id(+);

            END;
            """;

List<KnowledgeRelation> relations =
        graphRelationExtractor.extractKnowledgeRelations(
                sourceCode,
                "SP_REPORTE_PRODUCTOS",
                List.of());

        // =========================================
        // EXPECTED TABLES
        // =========================================

        assertTrue(

                relations.stream().anyMatch(

                        r ->
                                r.relation().equals("READS")
                                        && r.target().equals("PRODUCTOS")));

        assertTrue(

                relations.stream().anyMatch(

                        r ->
                                r.relation().equals("READS")
                                        && r.target().equals("CATEGORIAS")));

        assertTrue(

                relations.stream().anyMatch(

                        r ->
                                r.relation().equals("READS")
                                        && r.target().equals("STOCK_DEPOSITO")));

        // =========================================
        // GHOST NODES MUST NOT EXIST
        // =========================================

        assertFalse(

                relations.stream().anyMatch(

                        r -> r.target().equals("S")));

        assertFalse(

                relations.stream().anyMatch(

                        r -> r.target().equals("EN")));

        assertFalse(

                relations.stream().anyMatch(

                        r -> r.target().equals("LEFT")));

        assertFalse(

                relations.stream().anyMatch(

                        r -> r.target().equals("JOIN")));

        assertFalse(

                relations.stream().anyMatch(

                        r -> r.target().equals("ORACLE")));

        // =========================================
        // NO INVALID CALLS
        // =========================================

        assertFalse(

                relations.stream().anyMatch(

                        r ->
                                r.relation().equals("CALLS")
                                        && r.target().equals("S")));
    }

    @Test
    void should_accept_prefixed_package_calls_and_ignore_non_code_calls() {
        String sourceCode = """
                CREATE OR REPLACE PROCEDURE SP_PROCESAR IS
                BEGIN
                    P_FACTURACION.PROCESAR();
                    R_REPORTES.GENERAR();
                    C_CLIENTES.ACTUALIZAR();
                    V_SERVICIOS.EJECUTAR();
                    -- P_FANTASMA.EJECUTAR();
                    V_TEXTO := 'R_FANTASMA.GENERAR()';
                END;
                """;

        List<String> calls = graphRelationExtractor.extractKnowledgeRelations(
                        sourceCode, "SP_PROCESAR", List.of())
                .stream()
                .filter(relation -> relation.relation().equals("CALLS"))
                .map(KnowledgeRelation::target)
                .toList();

        assertTrue(calls.containsAll(List.of(
                "P_FACTURACION", "R_REPORTES", "C_CLIENTES", "V_SERVICIOS")));
        assertFalse(calls.contains("P_FANTASMA"));
        assertFalse(calls.contains("R_FANTASMA"));
    }

    @Test
    void should_extract_schema_and_database_link_table_targets() {
        String sourceCode = """
                CREATE OR REPLACE PROCEDURE SP_REMOTO IS
                BEGIN
                    SELECT * FROM CRM.CLIENTES@REMOTE_DB;
                    INSERT INTO ERP.AUDITORIA@REMOTE_DB (ID) VALUES (1);
                END;
                """;

        List<KnowledgeRelation> relations = graphRelationExtractor.extractKnowledgeRelations(
                sourceCode, "SP_REMOTO", List.of());

        assertTrue(relations.stream().anyMatch(relation ->
                relation.relation().equals("READS")
                        && relation.target().equals("CRM.CLIENTES@REMOTE_DB")));
        assertTrue(relations.stream().anyMatch(relation ->
                relation.relation().equals("WRITES")
                        && relation.target().equals("ERP.AUDITORIA@REMOTE_DB")));
    }
}
