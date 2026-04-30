package com.ignacio.legacyanalyzer.application.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.ignacio.legacyanalyzer.application.dto.AnalyzeLegacyResponse;
import com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence.LegacyObjectEntity;

class LegacyObjectMapperTest {

    private final LegacyObjectMapper mapper = new LegacyObjectMapper();

    @Test
    void shouldMapEntityToResponseCorrectly() {

        LegacyObjectEntity entity = new LegacyObjectEntity();
        entity.setName("TEST_PROC");
        entity.setType("PROCEDURE");
        entity.setProcedures("A,B");
        entity.setReferencedTables("T1,T2");
        entity.setCodeSmells("SELECT *,COMMIT");
        entity.setRiskScore(5);
        entity.setRiskLevel("MEDIUM");
        entity.setFunctionalSummary("summary");

        AnalyzeLegacyResponse response = mapper.toResponse(entity);

        assertEquals("TEST_PROC", response.name());
        assertEquals(List.of("A", "B"), response.procedures());
        assertEquals(List.of("T1", "T2"), response.referencedTables());
        assertEquals(List.of("SELECT *", "COMMIT"), response.codeSmells());
        assertEquals(5, response.riskScore());
        assertEquals("MEDIUM", response.riskLevel());
    }

    @Test
    void shouldHandleNullValues() {

        LegacyObjectEntity entity = new LegacyObjectEntity();
        entity.setName("TEST");
        entity.setType("PROCEDURE");

        AnalyzeLegacyResponse response = mapper.toResponse(entity);

        assertEquals(List.of(), response.procedures());
        assertEquals(List.of(), response.referencedTables());
        assertEquals(List.of(), response.codeSmells());
        assertEquals(0, response.riskScore());
        assertEquals("LOW", response.riskLevel());
    }
}