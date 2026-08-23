package com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class KnowledgeRelationRepositoryTest {

    @Autowired
    private KnowledgeRelationRepository repository;

    @Test
    void shouldPersistEvidenceAndRejectDuplicateIdentityWithinAnalysis() {
        KnowledgeRelationEntity relation = relation("analysis-123", "SELECT * FROM CLIENTES;");

        KnowledgeRelationEntity saved = repository.saveAndFlush(relation);

        assertEquals("PKG_VENTAS.SP_CALCULAR", saved.getSourceObject());
        assertEquals(10, saved.getSourceLineStart());
        assertEquals(12, saved.getSourceLineEnd());
        assertEquals("SELECT * FROM CLIENTES;", saved.getCodeSnippet());
        assertEquals(0.8d, saved.getConfidenceLevel());
        assertEquals("analysis-123", saved.getAnalysisId());

        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(
                        relation("analysis-123", "otro snippet de la misma relación")));
    }

    private KnowledgeRelationEntity relation(String analysisId, String snippet) {
        return new KnowledgeRelationEntity(
                "PKG_VENTAS", "READS", "CLIENTES",
                "PKG_VENTAS.SP_CALCULAR", 10, 12,
                snippet, 0.8d, analysisId);
    }
}
