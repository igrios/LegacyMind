package com.ignacio.legacyanalyzer.domain.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.ignacio.legacyanalyzer.domain.model.ExceptionMetadata;

class ExceptionSemanticExtractorTest {

    @Test
    void shouldDetectOracleExceptions() {

        String sql = """
                EXCEPTION
                    WHEN NO_DATA_FOUND THEN NULL;
                    WHEN DUP_VAL_ON_INDEX THEN NULL;
                    WHEN OTHERS THEN NULL;
                """;

        ExceptionSemanticExtractor extractor =
                new ExceptionSemanticExtractor();

        List<ExceptionMetadata> result =
                extractor.extract(sql);

        assertEquals(3, result.size());

        assertTrue(
                result.stream()
                        .anyMatch(e ->
                                e.exceptionName().equals("NO_DATA_FOUND")));

        assertTrue(
                result.stream()
                        .anyMatch(e ->
                                e.exceptionName().equals("DUP_VAL_ON_INDEX")));

        assertTrue(
                result.stream()
                        .anyMatch(e ->
                                e.exceptionName().equals("OTHERS")));
    }
}