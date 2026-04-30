package com.ignacio.legacyanalyzer.application.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.ignacio.legacyanalyzer.application.dto.AnalyzeLegacyResponse;
import com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence.LegacyObjectEntity;

class LegacyObjectMapperTest {

    private final LegacyObjectMapper mapper = new LegacyObjectMapper();

    @Test
    void shouldMapEntityToResponseCorrectly() {

        LegacyObjectEntity entity = new LegacyObjectEntity(
                "1",
                "TEST_PROC",
                "PROCEDURE",
                "source",
                "A,B",
                "T1,T2",
                "SELECT *,COMMIT",
                5,
                "MEDIUM",
                "summary",
                LocalDateTime.now()
        );

        AnalyzeLegacyResponse response = mapper.toResponse(entity);

        assertEquals("TEST_PROC", response.name());
        assertEquals("PROCEDURE", response.type());
        assertEquals(List.of("A", "B"), response.procedures());
        assertEquals(List.of("T1", "T2"), response.referencedTables());
        assertEquals(List.of("SELECT *", "COMMIT"), response.codeSmells());
        assertEquals(5, response.riskScore());
        assertEquals("MEDIUM", response.riskLevel());
        assertEquals("summary", response.functionalSummary());
    }

    @Test
    void shouldHandleNullValues() {

        LegacyObjectEntity entity = new LegacyObjectEntity(
                "1",
                "TEST_PROC",
                "PROCEDURE",
                "source",
                null,
                null,
                null,
                null,
                null,
                null,
                LocalDateTime.now()
        );

        AnalyzeLegacyResponse response = mapper.toResponse(entity);

        assertEquals(List.of(), response.procedures());
        assertEquals(List.of(), response.referencedTables());
        assertEquals(List.of(), response.codeSmells());
        assertEquals(0, response.riskScore());
        assertEquals("LOW", response.riskLevel());
        assertEquals("No summary available", response.functionalSummary());
    }
}