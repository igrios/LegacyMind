package com.ignacio.legacyanalyzer.application.usecase;

import java.util.List;
import com.ignacio.legacyanalyzer.domain.model.LegacyObject;
import com.ignacio.legacyanalyzer.domain.ports.TableDependencyRepositoryPort;
import com.ignacio.legacyanalyzer.domain.services.DependencyAnalyzerService;
import com.ignacio.legacyanalyzer.domain.model.TableDependency;




public class AnalyzeLegacyUseCase {

  

  private final TableDependencyRepositoryPort dependencyRepositoryPort;
  private final DependencyAnalyzerService analyzerService;

  public AnalyzeLegacyUseCase(TableDependencyRepositoryPort dependencyRepositoryPort, DependencyAnalyzerService dependencyAnalyzerService) {
    this.dependencyRepositoryPort = dependencyRepositoryPort;
    this.analyzerService = dependencyAnalyzerService;
  } 


  public void processDependencies(LegacyObject object){


     List<TableDependency> deps =
                analyzerService.buildDependencies(
                        object.getReferencedTables(),
                        object.getName()
                );

    dependencyRepositoryPort.saveAll(deps);  

  }

 


}