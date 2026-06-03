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

        assertEquals("TEST", response.name());

        assertEquals("PROCEDURE", response.type());

        assertEquals(
                List.of("PROC1", "PROC2"),
                response.procedures());

        assertEquals(
                List.of("TABLE1", "TABLE2"),
                response.referencedTables());

        assertEquals(
                List.of("COMMIT_USAGE", "GENERIC_EXCEPTION"),
                response.codeSmells());

        assertEquals(
                5,
                response.riskScore());

        assertEquals(
                "MEDIUM",
                response.riskLevel());

        assertEquals(
                "summary",
                response.functionalSummary());
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
                response.procedures());

        assertEquals(
                List.of(),
                response.referencedTables());

        assertEquals(
                List.of(),
                response.codeSmells());

        assertEquals(
                0,
                response.riskScore());

        assertEquals(
                "LOW",
                response.riskLevel());

        assertEquals(
                "No summary available",
                response.functionalSummary());
    }
}