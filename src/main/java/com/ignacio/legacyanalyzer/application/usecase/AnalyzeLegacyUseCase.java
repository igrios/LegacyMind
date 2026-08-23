package com.ignacio.legacyanalyzer.application.usecase;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ignacio.legacyanalyzer.domain.model.LegacyObject;
import com.ignacio.legacyanalyzer.domain.model.TableDependency;
import com.ignacio.legacyanalyzer.domain.ports.LegacyParserPort;
import com.ignacio.legacyanalyzer.domain.ports.PersistAnalysisPort;
import com.ignacio.legacyanalyzer.domain.services.DependencyAnalyzerService;

@Service
public class AnalyzeLegacyUseCase {

    private final LegacyParserPort parserPort;
    private final PersistAnalysisPort persistAnalysisPort;
    private final DependencyAnalyzerService analyzerService;

    public AnalyzeLegacyUseCase(
            LegacyParserPort parserPort,
            PersistAnalysisPort persistAnalysisPort,
            DependencyAnalyzerService analyzerService) {

        this.parserPort = parserPort;
        this.persistAnalysisPort = persistAnalysisPort;
        this.analyzerService = analyzerService;
    }

    @Transactional
    public LegacyObject execute(String sourceCode) {
        LegacyObject object = parserPort.parse(sourceCode);

        List<String> relations = parserPort.extractSemanticRelations(sourceCode);
        List<TableDependency> dependencies =
                analyzerService.buildFromRelations(relations, object.getName());

        persistAnalysisPort.persist(object, dependencies);

        return object;
    }
}
