package com.ignacio.legacyanalyzer.infrastructure.adapters.input.controller;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ignacio.legacyanalyzer.application.dto.AnalyzeLegacyRequest;
import com.ignacio.legacyanalyzer.application.dto.AnalyzeLegacyResponse;
import com.ignacio.legacyanalyzer.application.dto.ImpactAnalysisResponse;
import com.ignacio.legacyanalyzer.application.dto.MetadataResponse;
import com.ignacio.legacyanalyzer.application.mapper.LegacyObjectMapper;
import com.ignacio.legacyanalyzer.application.usecase.DeleteDatabaseUseCase;
import com.ignacio.legacyanalyzer.application.usecase.GetImpactByLevelsUseCase;
import com.ignacio.legacyanalyzer.application.usecase.GetImpactGraphUseCase;
import com.ignacio.legacyanalyzer.application.usecase.GetImpactUseCase;
import com.ignacio.legacyanalyzer.application.usecase.GetKnowledgeGraphUseCase;
import com.ignacio.legacyanalyzer.domain.model.BusinessRuleMetadata;
import com.ignacio.legacyanalyzer.domain.model.CursorMetadata;
import com.ignacio.legacyanalyzer.domain.model.DbLinkMetadata;
import com.ignacio.legacyanalyzer.domain.model.ExceptionMetadata;
import com.ignacio.legacyanalyzer.domain.model.KnowledgeRelation;
import com.ignacio.legacyanalyzer.domain.model.LegacyObject;
import com.ignacio.legacyanalyzer.domain.model.SubprogramNode;
import com.ignacio.legacyanalyzer.domain.model.TableDependency;
import com.ignacio.legacyanalyzer.domain.ports.TableDependencyRepositoryPort;
import com.ignacio.legacyanalyzer.domain.services.DependencyAnalyzerService;
import com.ignacio.legacyanalyzer.domain.services.ImpactAnalysisService;
import com.ignacio.legacyanalyzer.domain.services.ImpactAnalyzer;
import com.ignacio.legacyanalyzer.infrastructure.adapters.output.parser.RegexLegacyParserAdapter;
import com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence.KnowledgeRelationEntity;
import com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence.KnowledgeRelationRepository;
import com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence.LegacyObjectRepository;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/legacy")
public class LegacyController {

        private final RegexLegacyParserAdapter parserAdapter;
        private final LegacyObjectRepository repository;
        private final GetImpactUseCase getImpactUseCase;
        private final TableDependencyRepositoryPort dependencyPort;
        private final DependencyAnalyzerService analyzerService;
        private final GetImpactByLevelsUseCase getImpactByLevelsUseCase;
        private final ImpactAnalysisService impactService;
        private final GetImpactGraphUseCase getImpactGraphUseCase;
        private final KnowledgeRelationRepository knowledgeRelationRepository;
        private final DeleteDatabaseUseCase deleteDatabaseUseCase;
        private final GetKnowledgeGraphUseCase getKnowledgeGraphUseCase;

        public LegacyController(LegacyObjectRepository repository,
                        RegexLegacyParserAdapter parserAdapter, GetImpactUseCase getImpactUseCase,
                        TableDependencyRepositoryPort dependencyPort,
                        DependencyAnalyzerService analyzerService,
                        GetImpactByLevelsUseCase getImpactByLevelsUseCase,
                        ImpactAnalysisService impactService,
                        GetImpactGraphUseCase getImpactGraphUseCase,
                        KnowledgeRelationRepository knowledgeRelationRepository,
                        DeleteDatabaseUseCase deleteDatabaseUseCase,
                        GetKnowledgeGraphUseCase getKnowledgeGraphUseCase) {

                this.repository = repository;
                this.parserAdapter = parserAdapter;
                this.getImpactUseCase = getImpactUseCase;
                this.dependencyPort = dependencyPort;
                this.analyzerService = analyzerService;
                this.getImpactByLevelsUseCase = getImpactByLevelsUseCase;
                this.impactService = impactService;
                this.getImpactGraphUseCase = getImpactGraphUseCase;
                this.knowledgeRelationRepository = knowledgeRelationRepository;
                this.deleteDatabaseUseCase = deleteDatabaseUseCase;
                this.getKnowledgeGraphUseCase = getKnowledgeGraphUseCase;
        }


        @PostMapping("/analyze")
        public ResponseEntity<AnalyzeLegacyResponse> analyze(
                        @RequestBody AnalyzeLegacyRequest request) {

                if (request.getSourceCode() == null || request.getSourceCode().isBlank()) {

                        return ResponseEntity.badRequest().build();
                }

                LegacyObject object = parserAdapter.parse(request.getSourceCode());

                object.getKnowledgeRelations().forEach(relation -> {

                        boolean exists = knowledgeRelationRepository
                                        .existsBySourceAndRelationAndTarget(

                                                        relation.source(),

                                                        relation.relation(),

                                                        relation.target());

                        if (!exists) {

                                knowledgeRelationRepository.save(

                                                new KnowledgeRelationEntity(

                                                                relation.source(),

                                                                relation.relation(),

                                                                relation.target()));
                        }
                });


                List<String> relations =
                                parserAdapter.extractSemanticRelations(request.getSourceCode());

                List<TableDependency> dependencies =
                                analyzerService.buildFromRelations(relations, object.getName());

                if (dependencies != null) {
                        dependencyPort.saveAllDependencies(dependencies);
                }

                LegacyObjectMapper mapper = new LegacyObjectMapper();

                repository.save(mapper.toEntity(object));

                return ResponseEntity.ok(new AnalyzeLegacyResponse(object.getName(),
                                object.getType(), object.getProcedures(),
                                object.getReferencedTables(), object.getCodeSmells(),
                                object.getRiskScore(), object.getRiskLevel(),
                                object.getFunctionalSummary(), object.getSubprograms(),
                                object.getCursors(), object.getExceptions(), object.getDbLinks(),
                                object.getBusinessRules(), object.getKnowledgeRelations()));
        }

        @GetMapping("/history")
        public List<AnalyzeLegacyResponse> history() {

                return repository.findAll().stream().map(entity -> new AnalyzeLegacyResponse(

                                entity.getName(),

                                entity.getType(),

                                entity.getProcedures() != null
                                                ? List.of(entity.getProcedures().split(","))
                                                : List.of(),

                                entity.getReferencedTables() != null
                                                ? List.of(entity.getReferencedTables().split(","))
                                                : List.of(),

                                entity.getCodeSmells() != null
                                                ? List.of(entity.getCodeSmells().split(","))
                                                : List.of(),

                                entity.getRiskScore() != null ? entity.getRiskScore() : 0,

                                entity.getRiskLevel() != null ? entity.getRiskLevel() : "LOW",

                                entity.getFunctionalSummary() != null
                                                ? entity.getFunctionalSummary()
                                                : "No summary available",

                                List.<SubprogramNode>of(), // subprograms

                                List.<CursorMetadata>of(), // cursors

                                List.<ExceptionMetadata>of(), // exceptions

                                List.<DbLinkMetadata>of(), // dbLinks

                                List.<BusinessRuleMetadata>of(), // businessRules

                                List.<KnowledgeRelation>of() // knowledgeRelations
                )).toList();
        }

        @GetMapping("/impact/{table}")
        public ResponseEntity<Set<String>> getImpact(@PathVariable String table) {
                Set<String> result = getImpactUseCase.execute(table);
                return ResponseEntity.ok(result);
        }

        @GetMapping("/impact/levels/{table}")
        public ResponseEntity<Map<Integer, Set<String>>> getImpactByLevels(
                        @PathVariable String table) {
                return ResponseEntity.ok(getImpactByLevelsUseCase.execute(table));
        }

        @GetMapping("/impact/paths/{table}")
        public ResponseEntity<List<List<String>>> getPaths(@PathVariable String table) {
                return ResponseEntity.ok(impactService.getAllPaths(table));
        }

        @GetMapping("/impact/graph/{table}")
        public Map<String, Object> getGraph(@PathVariable String table) {
                return getImpactGraphUseCase.execute(table);
        }

        @GetMapping("/knowledge-graph")
        public Map<String, Object> getKnowledgeGraph() {

                return getKnowledgeGraphUseCase.execute();
        }


        @DeleteMapping("/database")
        public ResponseEntity<String> clearDatabase() {

                deleteDatabaseUseCase.execute();

                return ResponseEntity.ok("Database cleaned successfully");
        }

        @GetMapping("/object/{name}")
        public ResponseEntity<AnalyzeLegacyResponse> getObject(@PathVariable String name) {

                LegacyObjectMapper mapper = new LegacyObjectMapper();

                return repository.findByName(name)

                                .map(entity -> ResponseEntity.ok(mapper.toResponse(entity)))

                                .orElse(ResponseEntity.notFound().build());
        }


        @GetMapping("/metadata/{name}")
        public ResponseEntity<MetadataResponse> getMetadata(@PathVariable String name) {

                LegacyObjectMapper mapper = new LegacyObjectMapper();

                return repository.findByName(name)

                                .map(entity ->

                                ResponseEntity.ok(mapper.toMetadataResponse(entity)))

                                .orElse(ResponseEntity.notFound().build());
        }


        @RestController
        @RequestMapping("/api/test")
        public class ImpactTestController {

                private final ImpactAnalyzer impactAnalyzer;

                public ImpactTestController(ImpactAnalyzer impactAnalyzer) {
                        this.impactAnalyzer = impactAnalyzer;
                }

                @GetMapping("/impact/{objectName}")
                public ImpactAnalysisResponse impact(@PathVariable String objectName) {

                        return impactAnalyzer.analyze(objectName);
                }
        }



}
