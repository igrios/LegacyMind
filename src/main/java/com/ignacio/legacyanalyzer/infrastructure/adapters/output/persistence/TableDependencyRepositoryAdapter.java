package com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence;

import java.util.List;
import org.springframework.stereotype.Component;
import com.ignacio.legacyanalyzer.domain.model.TableDependency;
import com.ignacio.legacyanalyzer.domain.ports.TableDependencyRepositoryPort;



@Component
public class TableDependencyRepositoryAdapter implements TableDependencyRepositoryPort {

  private final TableDependencyJpaRepository repository;

  public TableDependencyRepositoryAdapter(TableDependencyJpaRepository repository) {
    this.repository = repository;
  }

  @Override
  public void saveAll(List<TableDependency> dependencies) {

    List<TableDependencyEntity> entities = dependencies.stream().map(
        d -> new TableDependencyEntity(d.getSourceTable(), d.getTargetTable(), d.getObjectName()))
        .toList();

    repository.saveAll(entities);


  }

  @Override
  public List<TableDependency> findBySourceTable(String table) {
    return repository.findBySourceTable(table).stream()
        .map(e -> new TableDependency(e.getSourceTable(), e.getTargetTable(), e.getObjectName()))
        .toList();
  }



}
