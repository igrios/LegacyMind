package com.ignacio.legacyanalyzer.infrastructure.adapters.input.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ignacio.legacyanalyzer.application.dto.AnalyzeLegacyRequest;
import com.ignacio.legacyanalyzer.application.dto.AnalyzeLegacyResponse;
import com.ignacio.legacyanalyzer.application.usecase.GetImpactByLevelsUseCase;
import com.ignacio.legacyanalyzer.application.usecase.GetImpactUseCase;
import com.ignacio.legacyanalyzer.domain.model.LegacyObject;
import com.ignacio.legacyanalyzer.domain.ports.TableDependencyRepositoryPort;
import com.ignacio.legacyanalyzer.domain.services.DependencyAnalyzerService;
import com.ignacio.legacyanalyzer.domain.services.ImpactAnalysisService;
import com.ignacio.legacyanalyzer.infrastructure.adapters.output.parser.RegexLegacyParserAdapter;
import com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence.LegacyObjectEntity;
import com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence.LegacyObjectRepository;

@ExtendWith(MockitoExtension.class)
class LegacyControllerTest {

    @Mock
    private LegacyObjectRepository repository;

    @Mock
    private RegexLegacyParserAdapter parserAdapter;

    @Mock
    private TableDependencyRepositoryPort dependencyPort;

    @Mock
    private DependencyAnalyzerService analyzerService;

    @Mock
    private GetImpactUseCase getImpactUseCase;

    @Mock
    private GetImpactByLevelsUseCase getImpactByLevelsUseCase;

    @Mock
    private ImpactAnalysisService impactService;

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

      LegacyObject mockObject = org.mockito.Mockito.mock(LegacyObject.class);

when(mockObject.getName()).thenReturn("TEST_PROC");
when(mockObject.getType()).thenReturn("PROCEDURE");
when(mockObject.getSourceCode()).thenReturn(request.getSourceCode());
when(mockObject.getProcedures()).thenReturn(java.util.List.of());
when(mockObject.getReferencedTables()).thenReturn(java.util.List.of("USERS"));
when(mockObject.getCodeSmells()).thenReturn(java.util.List.of());
when(mockObject.getRiskScore()).thenReturn(0);
when(mockObject.getRiskLevel()).thenReturn("LOW");
when(mockObject.getFunctionalSummary()).thenReturn("summary");
        

        when(parserAdapter.parse(anyString())).thenReturn(mockObject);
        when(parserAdapter.extractSemanticRelations(anyString()))
                .thenReturn(List.of("USERS->TEST"));

        when(analyzerService.buildFromRelations(anyList(), anyString()))
                .thenReturn(List.of());

        // 🔥 Ejecutar
        AnalyzeLegacyResponse response = controller.analyze(request);

        // 🔥 Verificar persistencia
        verify(repository).save(any(LegacyObjectEntity.class));

        // 🔥 Validaciones
        assertNotNull(response);
        assertEquals("TEST_PROC", response.name());
        assertEquals("PROCEDURE", response.type());
    }
}