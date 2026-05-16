package com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence;

import java.util.List;
import org.springframework.stereotype.Repository;
import com.ignacio.legacyanalyzer.domain.model.TableDependency;
import com.ignacio.legacyanalyzer.domain.ports.TableDependencyRepositoryPort;

  

@Repository
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

@Override
  public List<String> findTargetsBySource(String sourceTable) {
    return repository.findBySourceTable(sourceTable).stream()
        .map(TableDependencyEntity::getTargetTable)
        .distinct()
        .toList();
  }

  @Override
  public void save(TableDependency dependency) {
    TableDependencyEntity entity = new TableDependencyEntity(
        dependency.getSourceTable(),
        dependency.getTargetTable(),
        dependency.getObjectName()
    );
    repository.save(entity);
  }
  
@Override
public void saveAllDependencies(List<TableDependency> dependencies) {

    List<TableDependencyEntity> entities = dependencies.stream()
        .map(d -> new TableDependencyEntity(
            d.getSourceTable(),
            d.getTargetTable(),
            d.getObjectName()
        ))
        .toList();

    repository.saveAll(entities);
}
@Override
public void deleteAll() {

    repository.deleteAll();
}

}
