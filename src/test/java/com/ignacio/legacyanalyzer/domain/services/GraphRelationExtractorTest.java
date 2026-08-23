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
}
