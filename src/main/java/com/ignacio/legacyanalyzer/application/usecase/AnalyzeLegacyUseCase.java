package com.ignacio.legacyanalyzer.application.usecase;

import java.util.List;
import org.springframework.stereotype.Service;
import com.ignacio.legacyanalyzer.domain.model.LegacyObject;
import com.ignacio.legacyanalyzer.domain.model.TableDependency;
import com.ignacio.legacyanalyzer.domain.ports.TableDependencyRepositoryPort;
import com.ignacio.legacyanalyzer.domain.services.DependencyAnalyzerService;
import com.ignacio.legacyanalyzer.infrastructure.adapters.output.parser.RegexLegacyParserAdapter;

@Service
public class AnalyzeLegacyUseCase {

    private final TableDependencyRepositoryPort dependencyRepositoryPort;
    private final RegexLegacyParserAdapter parserAdapter;
    private final DependencyAnalyzerService analyzerService;

    public AnalyzeLegacyUseCase(
            TableDependencyRepositoryPort dependencyRepositoryPort,
            RegexLegacyParserAdapter parserAdapter,
            DependencyAnalyzerService analyzerService) {

        this.dependencyRepositoryPort = dependencyRepositoryPort;
        this.parserAdapter = parserAdapter;
        this.analyzerService = analyzerService;
    }

    public void processDependencies(LegacyObject object, String sourceCode) {

        List<String> relations =
                parserAdapter.extractSemanticRelations(sourceCode);

        List<TableDependency> deps =
                analyzerService.buildFromRelations(relations, object.getName());

        dependencyRepositoryPort.saveAll(deps);
    }
}