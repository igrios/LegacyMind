package com.ignacio.legacyanalyzer.domain.ports;

import com.ignacio.legacyanalyzer.domain.model.TableDependency;
import java.util.List;

public interface TableDependencyRepositoryPort {

    void save(TableDependency dependency);

    void saveAll(List<TableDependency> dependencies); // 🔥 ESTE NOMBRE

   List<TableDependency> findBySourceTable(String table);

    List<String> findTargetsBySource(String sourceTable);
}
