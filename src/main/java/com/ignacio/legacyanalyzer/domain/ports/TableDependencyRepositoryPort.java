package com.ignacio.legacyanalyzer.domain.ports;

import java.util.List;
import com.ignacio.legacyanalyzer.domain.model.TableDependency;

public interface TableDependencyRepositoryPort {

    void save(TableDependency dependency);

    void saveAll(List<TableDependency> dependencies); // 🔥 ESTE NOMBRE

    List<TableDependency> findBySourceTable(String table);

    List<String> findTargetsBySource(String sourceTable);
    void saveAllDependencies(List<TableDependency> dependencies);
    void deleteAll();
}
