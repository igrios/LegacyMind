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

                "TEST",

                "PROCEDURE",

                "SOURCE",

                "PROC1,PROC2",

                "TABLE1,TABLE2",

                "COMMIT_USAGE,GENERIC_EXCEPTION",

                5,

                "MEDIUM",

                "summary",

                List.of(), // cursors

                List.of(), // business rules

                List.of(), // subprograms
                
                List.of(), // exceptions

                List.of(), // db links

                LocalDateTime.now());

        AnalyzeLegacyResponse response =
                mapper.toResponse(entity);

        assertEquals("TEST", response.getName());

        assertEquals("PROCEDURE", response.getType());

        assertEquals(
                List.of("PROC1", "PROC2"),
                response.getProcedures());

        assertEquals(
                List.of("TABLE1", "TABLE2"),
                response.getReferencedTables());

        assertEquals(
                List.of("COMMIT_USAGE", "GENERIC_EXCEPTION"),
                response.getCodeSmells());

        assertEquals(
                5,
                response.getRiskScore());

        assertEquals(
                "MEDIUM",
                response.getRiskLevel());

        assertEquals(
                "summary",
                response.getFunctionalSummary());
    }

    @Test
    void shouldHandleNullValues() {

        LegacyObjectEntity entity = new LegacyObjectEntity(

                "1",

                "TEST",

                "PROCEDURE",

                "SOURCE",

                null,

                null,

                null,

                null,

                null,

                null,

                List.of(), // cursors

                List.of(), // business rules

                List.of(), // subprograms

                List.of(), // exceptions

                List.of(), // db links

                LocalDateTime.now());

        AnalyzeLegacyResponse response =
                mapper.toResponse(entity);

        assertEquals(
                List.of(),
                response.getProcedures());

        assertEquals(
                List.of(),
                response.getReferencedTables());

        assertEquals(
                List.of(),
                response.getCodeSmells());

        assertEquals(
                0,
                response.getRiskScore());

        assertEquals(
                "LOW",
                response.getRiskLevel());

        assertEquals(
                "No summary available",
                response.getFunctionalSummary());
    }
}
