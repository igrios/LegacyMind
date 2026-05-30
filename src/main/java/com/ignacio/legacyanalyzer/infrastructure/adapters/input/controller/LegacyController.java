package com.ignacio.legacyanalyzer.infrastructure.adapters.input.controller;

import java.util.HashMap;
import java.util.HashSet;
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
import com.ignacio.legacyanalyzer.application.mapper.LegacyObjectMapper;
import com.ignacio.legacyanalyzer.application.usecase.DeleteDatabaseUseCase;
import com.ignacio.legacyanalyzer.application.usecase.GetImpactByLevelsUseCase;
import com.ignacio.legacyanalyzer.application.usecase.GetImpactGraphUseCase;
import com.ignacio.legacyanalyzer.application.usecase.GetImpactUseCase;
import com.ignacio.legacyanalyzer.domain.model.LegacyObject;
import com.ignacio.legacyanalyzer.domain.model.TableDependency;
import com.ignacio.legacyanalyzer.domain.ports.TableDependencyRepositoryPort;
import com.ignacio.legacyanalyzer.domain.services.DependencyAnalyzerService;
import com.ignacio.legacyanalyzer.domain.services.ImpactAnalysisService;
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

        public LegacyController(LegacyObjectRepository repository,
                        RegexLegacyParserAdapter parserAdapter, GetImpactUseCase getImpactUseCase,
                        TableDependencyRepositoryPort dependencyPort,
                        DependencyAnalyzerService analyzerService,
                        GetImpactByLevelsUseCase getImpactByLevelsUseCase,
                        ImpactAnalysisService impactService,
                        GetImpactGraphUseCase getImpactGraphUseCase,
                        KnowledgeRelationRepository knowledgeRelationRepository,
                        DeleteDatabaseUseCase deleteDatabaseUseCase) {

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
        }

        @PostMapping("/analyze")
        public AnalyzeLegacyResponse analyze(@RequestBody AnalyzeLegacyRequest request) {

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

                return new AnalyzeLegacyResponse(

                                object.getName(),

                                object.getType(),

                                object.getProcedures(),

                                object.getReferencedTables(),

                                object.getCodeSmells(),

                                object.getRiskScore(),

                                object.getRiskLevel(),

                                object.getFunctionalSummary(),

                                object.getSubprograms(),

                                object.getBusinessRules(),

                                object.getKnowledgeRelations());
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

                                List.of(), // subprograms

                                List.of(), // businessRules

                                List.of() // knowledgeRelations

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

                List<KnowledgeRelationEntity> relations = knowledgeRelationRepository.findAll();

                Set<String> nodes = new HashSet<>();

                List<Map<String, String>> edges = relations.stream().map(relation -> {

                        nodes.add(relation.getSource());

                        nodes.add(relation.getTarget());

                        Map<String, String> edge = new HashMap<>();

                        edge.put("source", relation.getSource());

                        edge.put("target", relation.getTarget());

                        edge.put("relation", relation.getRelation());

                        return edge;
                }).toList();

                Map<String, Object> result = new HashMap<>();

                result.put("nodes", nodes);

                result.put("edges", edges);

                return result;
        }


        @DeleteMapping("/database")
        public ResponseEntity<String> clearDatabase() {

                deleteDatabaseUseCase.execute();

                return ResponseEntity.ok("Database cleaned successfully");
        }



}
