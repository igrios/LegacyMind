package com.ignacio.legacyanalyzer.infrastructure.adapters.input.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ignacio.legacyanalyzer.application.dto.AnalyzeLegacyRequest;
import com.ignacio.legacyanalyzer.application.dto.AnalyzeLegacyResponse;
import com.ignacio.legacyanalyzer.application.usecase.GetImpactByLevelsUseCase;
import com.ignacio.legacyanalyzer.application.usecase.GetImpactUseCase;
import com.ignacio.legacyanalyzer.domain.model.LegacyObject;
import com.ignacio.legacyanalyzer.domain.model.TableDependency;
import com.ignacio.legacyanalyzer.domain.ports.TableDependencyRepositoryPort;
import com.ignacio.legacyanalyzer.domain.services.DependencyAnalyzerService;
import com.ignacio.legacyanalyzer.infrastructure.adapters.output.parser.RegexLegacyParserAdapter;
import com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence.LegacyObjectEntity;
import com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence.LegacyObjectRepository;
import java.util.Map;

@RestController
@RequestMapping("/api/legacy")
public class LegacyController {

    private final RegexLegacyParserAdapter parserAdapter;
    private final LegacyObjectRepository repository;
    private final GetImpactUseCase getImpactUseCase;
    private final TableDependencyRepositoryPort dependencyPort;
    private final DependencyAnalyzerService analyzerService;
    private final GetImpactByLevelsUseCase getImpactByLevelsUseCase;


    public LegacyController(
            LegacyObjectRepository repository,
            RegexLegacyParserAdapter parserAdapter,
            GetImpactUseCase getImpactUseCase,
            TableDependencyRepositoryPort dependencyPort,
            DependencyAnalyzerService analyzerService,
            GetImpactByLevelsUseCase getImpactByLevelsUseCase) {

        this.repository = repository;
        this.parserAdapter = parserAdapter;
        this.getImpactUseCase = getImpactUseCase;   
        this.dependencyPort = dependencyPort;
        this.analyzerService = analyzerService;             
        this.getImpactByLevelsUseCase = getImpactByLevelsUseCase;

    }



        @PostMapping("/analyze")
        public AnalyzeLegacyResponse analyze(@RequestBody AnalyzeLegacyRequest request) {

               // 1. Parseo
    LegacyObject object = parserAdapter.parse(request.getSourceCode());

    // 🔥 2. GENERAR DEPENDENCIAS (NUEVO)
    List<TableDependency> dependencies =
            analyzerService.buildDependencies(
                    object.getReferencedTables(),
                    object.getName()
            );

    // 🔥 3. GUARDAR DEPENDENCIAS (NUEVO)
   dependencyPort.saveAll(dependencies);

    // 4. Persistir objeto (lo que ya tenías)
    LegacyObjectEntity entity = new LegacyObjectEntity(
            object.getId(),
            object.getName(),
            object.getType(),
            object.getSourceCode(),
            String.join(",", object.getProcedures()),
            String.join(",", object.getReferencedTables()),
            String.join(",", object.getCodeSmells()),
            object.getRiskScore(),
            object.getRiskLevel(),
            object.getFunctionalSummary(),
            LocalDateTime.now()
    );

    repository.save(entity);

    // 5. Response
    return new AnalyzeLegacyResponse(
            object.getName(),
            object.getType(),
            object.getProcedures(),
            object.getReferencedTables(),
            object.getCodeSmells(),
            object.getRiskScore(),
            object.getRiskLevel(),
            object.getFunctionalSummary()
    );
        }

        @GetMapping("/history")
        public List<AnalyzeLegacyResponse> history() {

                return repository.findAll().stream().map(entity -> new AnalyzeLegacyResponse(
                                entity.getName(), entity.getType(),

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
                                                : "No summary available"

                )).toList();
        }

  // Endpoint de impacto en cascada
    @GetMapping("/impact/{table}")
    public ResponseEntity<Set<String>> getImpact(@PathVariable String table) {
        Set<String> result = getImpactUseCase.execute(table);
        return ResponseEntity.ok(result);
    }


@GetMapping("/impact/levels/{table}")
public ResponseEntity<Map<Integer, Set<String>>> getImpactByLevels(@PathVariable String table) {
    return ResponseEntity.ok(getImpactByLevelsUseCase.execute(table));
}




}
