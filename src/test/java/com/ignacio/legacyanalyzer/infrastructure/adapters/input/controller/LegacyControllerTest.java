package com.ignacio.legacyanalyzer.infrastructure.adapters.input.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ignacio.legacyanalyzer.application.dto.AnalyzeLegacyRequest;
import com.ignacio.legacyanalyzer.application.dto.AnalyzeLegacyResponse;
import com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence.LegacyObjectEntity;
import com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence.LegacyObjectRepository;

@ExtendWith(MockitoExtension.class)
class LegacyControllerTest {

    @Mock
    private LegacyObjectRepository repository;

    @InjectMocks
    private LegacyController controller;

    @Test
    void analyzeShouldSaveAndReturnResponse() {

        AnalyzeLegacyRequest request = new AnalyzeLegacyRequest();
        request.setSourceCode("""
            CREATE OR REPLACE PROCEDURE test_proc IS
            BEGIN
                SELECT * FROM users;
            END;
        """);

        AnalyzeLegacyResponse response = controller.analyze(request);

        verify(repository).save(any(LegacyObjectEntity.class));

        assertNotNull(response);
        assertEquals("TEST_PROC", response.name());
        assertEquals("PROCEDURE", response.type());
    }
}