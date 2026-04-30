package com.ignacio.legacyanalyzer.infrastructure.adapters.input.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ignacio.legacyanalyzer.application.dto.AnalyzeLegacyRequest;
import com.ignacio.legacyanalyzer.application.dto.AnalyzeLegacyResponse;
import com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence.LegacyObjectEntity;
import com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence.LegacyObjectRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LegacyControllerTest {

    @Mock
    private LegacyObjectRepository repository;

    @InjectMocks
    private LegacyController controller;

    @Test
    void analyzeShouldSaveEntityAndReturnResponse() {
        AnalyzeLegacyRequest request = new AnalyzeLegacyRequest();
        request.setSourceCode("""
                CREATE OR REPLACE PROCEDURE x_proc IS
                BEGIN
                    SELECT * FROM users;
                    COMMIT;
                END;
                """);

        AnalyzeLegacyResponse response = controller.analyze(request);

        verify(repository, times(1)).save(any(LegacyObjectEntity.class));
        assertEquals("X_PROC", response.name());
        assertEquals("PROCEDURE", response.type());
        assertEquals(4, response.riskScore());
        assertEquals("MEDIUM", response.riskLevel());
        assertNotNull(response.codeSmells());
    }

    @Test
    void historyShouldMapEntitiesAndHandleNullValues() {
        LegacyObjectEntity entity = new LegacyObjectEntity(
                "1",
                "PKG_TEST",
                "PACKAGE",
                "source",
                "PROC_A,PROC_B",
                "TAB_A,TAB_B",
                null,
                null,
                null,
                null);

        when(repository.findAll()).thenReturn(List.of(entity));

        List<AnalyzeLegacyResponse> history = controller.history();

        assertEquals(1, history.size());
        AnalyzeLegacyResponse response = history.get(0);
        assertEquals("PKG_TEST", response.name());
        assertEquals(List.of("PROC_A", "PROC_B"), response.procedures());
        assertEquals(List.of("TAB_A", "TAB_B"), response.referencedTables());
        assertEquals(List.of(), response.codeSmells());
        assertEquals(0, response.riskScore());
        assertEquals("LOW", response.riskLevel());
    }
}
