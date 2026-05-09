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
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ignacio.legacyanalyzer.application.dto.AnalyzeLegacyRequest;
import com.ignacio.legacyanalyzer.application.dto.AnalyzeLegacyResponse;
import com.ignacio.legacyanalyzer.application.usecase.GetImpactByLevelsUseCase;
import com.ignacio.legacyanalyzer.application.usecase.GetImpactGraphUseCase;
import com.ignacio.legacyanalyzer.application.usecase.GetImpactUseCase;
import com.ignacio.legacyanalyzer.domain.model.LegacyObject;
import com.ignacio.legacyanalyzer.domain.model.TableDependency;
import com.ignacio.legacyanalyzer.domain.ports.TableDependencyRepositoryPort;
import com.ignacio.legacyanalyzer.domain.services.DependencyAnalyzerService;
import com.ignacio.legacyanalyzer.domain.services.ImpactAnalysisService;
import com.ignacio.legacyanalyzer.infrastructure.adapters.output.parser.RegexLegacyParserAdapter;
import com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence.LegacyObjectEntity;
import com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence.LegacyObjectRepository;

@ExtendWith(MockitoExtension.class)
class LegacyControllerTest {

    @Mock
    private RegexLegacyParserAdapter parserAdapter;

    @Mock
    private LegacyObjectRepository repository;

    @Mock
    private GetImpactUseCase getImpactUseCase;

    @Mock
    private TableDependencyRepositoryPort dependencyPort;

    @Mock
    private DependencyAnalyzerService analyzerService;

    @Mock
    private GetImpactByLevelsUseCase getImpactByLevelsUseCase;

    @Mock
    private ImpactAnalysisService impactService;

    @Mock
    private GetImpactGraphUseCase getImpactGraphUseCase;

    @InjectMocks
    private LegacyController controller;

    @SuppressWarnings("null")
@Test
    void analyzeShouldSaveAndReturnResponse() {

        String sourceCode = """
                CREATE OR REPLACE PROCEDURE test_proc IS
                BEGIN
                    SELECT * FROM users;
                END;
                """;

        AnalyzeLegacyRequest request = new AnalyzeLegacyRequest();

        request.setSourceCode(sourceCode);

        LegacyObject legacyObject = Mockito.mock(LegacyObject.class);

        when(legacyObject.getId()).thenReturn("1");

        when(legacyObject.getName()).thenReturn("TEST_PROC");

        when(legacyObject.getType()).thenReturn("PROCEDURE");

        when(legacyObject.getSourceCode()).thenReturn(sourceCode);

        when(legacyObject.getReferencedTables())
                .thenReturn(List.of("USERS"));

        when(legacyObject.getProcedures())
                .thenReturn(List.of());

        when(legacyObject.getCodeSmells())
                .thenReturn(List.of());

        when(legacyObject.getRiskScore())
                .thenReturn(0);

        when(legacyObject.getRiskLevel())
                .thenReturn("LOW");

        when(legacyObject.getFunctionalSummary())
                .thenReturn("Simple procedure");

        when(legacyObject.getSubprograms())
                .thenReturn(List.of());

        when(legacyObject.getKnowledgeRelations())
                .thenReturn(List.of());

        when(parserAdapter.parse(anyString()))
                .thenReturn(legacyObject);

        when(parserAdapter.extractSemanticRelations(anyString()))
                .thenReturn(List.of("TEST_PROC->USERS"));

        when(analyzerService.buildFromRelations(anyList(), anyString()))
                .thenReturn(List.<TableDependency>of());

        AnalyzeLegacyResponse response = controller.analyze(request);

        assertNotNull(response);

        assertEquals("TEST_PROC", response.name());

        assertEquals("PROCEDURE", response.type());

        assertEquals(1, response.referencedTables().size());

        assertEquals("USERS", response.referencedTables().get(0));

        verify(repository).save(any(LegacyObjectEntity.class));

        verify(dependencyPort).saveAllDependencies(anyList());
    }
}