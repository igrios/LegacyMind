package com.ignacio.legacyanalyzer.infrastructure.adapters.input.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

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
        request.setSourceCode("SELECT * FROM users;");

        AnalyzeLegacyResponse response = controller.analyze(request);

        verify(repository).save(any(LegacyObjectEntity.class));

        assertNotNull(response);
        assertNotNull(response.name());
        assertNotNull(response.type());
    }

    @Test
    void historyShouldReturnMappedResults() {
        LegacyObjectEntity entity = new LegacyObjectEntity(
                "1",
                "PKG_TEST",
                "PACKAGE",
                "source",
                "A,B",
                "T1,T2",
                null,
                null,
                null,
                null,
                LocalDateTime.now()
        );

        when(repository.findAll()).thenReturn(List.of(entity));

        List<AnalyzeLegacyResponse> result = controller.history();

        assertEquals(1, result.size());
        assertEquals("PKG_TEST", result.get(0).name());
        assertEquals(List.of("A", "B"), result.get(0).procedures());
    }
}
