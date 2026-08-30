package com.ignacio.legacyanalyzer.infrastructure.adapters.input.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import com.ignacio.legacyanalyzer.application.dto.AnalyzeLegacyRequest;
import com.ignacio.legacyanalyzer.application.dto.AnalyzeLegacyResponse;
import com.ignacio.legacyanalyzer.application.usecase.AnalyzeLegacyUseCase;
import com.ignacio.legacyanalyzer.application.usecase.GetImpactByLevelsUseCase;
import com.ignacio.legacyanalyzer.application.usecase.GetImpactGraphUseCase;
import com.ignacio.legacyanalyzer.application.usecase.GetImpactUseCase;
import com.ignacio.legacyanalyzer.domain.model.LegacyObject;
import com.ignacio.legacyanalyzer.domain.model.KnowledgeRelation;
import com.ignacio.legacyanalyzer.domain.services.graph.ImpactAnalysisService;
import com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence.LegacyObjectRepository;

@ExtendWith(MockitoExtension.class)
class LegacyControllerTest {

    @Mock
    private AnalyzeLegacyUseCase analyzeLegacyUseCase;

    @Mock
    private LegacyObjectRepository repository;

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

        when(legacyObject.getName()).thenReturn("TEST_PROC");

        when(legacyObject.getType()).thenReturn("PROCEDURE");

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
                .thenReturn(List.of(new KnowledgeRelation("TEST_PROC", "READS", "USERS")));

        when(analyzeLegacyUseCase.execute(anyString()))
                .thenReturn(legacyObject);

       ResponseEntity<AnalyzeLegacyResponse> response =  controller.analyze(request);

        assertNotNull(response);

        assertEquals("TEST_PROC", response.getBody().getName());

        assertEquals("PROCEDURE", response.getBody().getType());

        assertEquals(1, response.getBody().getReferencedTables().size());

        assertEquals("USERS", response.getBody().getReferencedTables().get(0));

        assertEquals(1, response.getBody().getKnowledgeRelations().size());

        verify(analyzeLegacyUseCase).execute(sourceCode);
    }

    @Test
    void analysisResponseShouldSerializeTheCompleteKnowledgeRelationsArray() throws Exception {
        AnalyzeLegacyResponse response = new AnalyzeLegacyResponse();
        response.setKnowledgeRelations(List.of(
                new KnowledgeRelation("PKG_ORDERS", "READS", "CUSTOMERS"),
                new KnowledgeRelation("PKG_ORDERS", "CALLS", "PKG_AUDIT.LOG_EVENT")));

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode payload = objectMapper.readTree(objectMapper.writeValueAsString(response));

        assertTrue(payload.has("knowledgeRelations"));
        assertEquals(2, payload.get("knowledgeRelations").size());
        assertEquals("PKG_ORDERS", payload.get("knowledgeRelations").get(0).get("source").asText());
        assertEquals("READS", payload.get("knowledgeRelations").get(0).get("relation").asText());
        assertEquals("CUSTOMERS", payload.get("knowledgeRelations").get(0).get("target").asText());
    }
}
