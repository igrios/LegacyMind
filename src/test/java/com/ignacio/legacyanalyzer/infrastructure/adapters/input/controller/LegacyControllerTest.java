package com.ignacio.legacyanalyzer.infrastructure.adapters.input.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ignacio.legacyanalyzer.application.dto.AnalyzeGraphResponse;
import com.ignacio.legacyanalyzer.application.dto.AnalyzeLegacyRequest;
import com.ignacio.legacyanalyzer.application.usecase.GetImpactByLevelsUseCase;
import com.ignacio.legacyanalyzer.application.usecase.GetImpactGraphUseCase;
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

    @Mock
    private GetImpactGraphUseCase getImpactGraphUseCase;

    @InjectMocks
    private LegacyController controller;

    @Test
    void analyzeShouldSaveAndReturnGraphResponse() {

        // 🔥 Request
        AnalyzeLegacyRequest request = new AnalyzeLegacyRequest();
        request.setSourceCode("""
                    CREATE OR REPLACE PROCEDURE test_proc IS
                    BEGIN
                        SELECT * FROM users;
                    END;
                """);

        // 🔥 Mock del objeto parseado
        LegacyObject mockObject = org.mockito.Mockito.mock(LegacyObject.class);

        when(mockObject.getName()).thenReturn("TEST_PROC");
        when(mockObject.getType()).thenReturn("PROCEDURE");
        when(mockObject.getSourceCode()).thenReturn(request.getSourceCode());
        when(mockObject.getProcedures()).thenReturn(List.of());
        when(mockObject.getReferencedTables()).thenReturn(List.of("USERS"));
        when(mockObject.getCodeSmells()).thenReturn(List.of());
        when(mockObject.getRiskScore()).thenReturn(0);
        when(mockObject.getRiskLevel()).thenReturn("LOW");
        when(mockObject.getFunctionalSummary()).thenReturn("summary");

        // 🔥 Parser
        when(parserAdapter.parse(anyString())).thenReturn(mockObject);
        when(parserAdapter.extractSemanticRelations(anyString()))
                .thenReturn(List.of("USERS->TEST"));

        // 🔥 Analyzer
        when(analyzerService.buildFromRelations(anyList(), anyString())).thenReturn(List.of());

        // 🔥 Graph mock
        when(getImpactGraphUseCase.execute(anyString()))
                .thenReturn(Map.of("nodes", List.of("TEST_PROC", "USERS"), "edges",
                        List.of(Map.of("from", "TEST_PROC", "to", "USERS"))));

        // 🔥 Ejecutar
        AnalyzeGraphResponse response = controller.analyze(request);

        // 🔥 Verificar persistencia
        verify(repository).save(any(LegacyObjectEntity.class));

        // 🔥 Validaciones
        assertNotNull(response);
        assertEquals("TEST_PROC", response.getName());
        assertEquals("PROCEDURE", response.getType());

        // 🔥 Grafo
        assertNotNull(response.getNodes());
        assertNotNull(response.getEdges());
        assertEquals(2, response.getNodes().size());
    }
}
