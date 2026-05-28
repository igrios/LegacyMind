package com.ignacio.legacyanalyzer.domain.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.ignacio.legacyanalyzer.domain.model.KnowledgeRelation;

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