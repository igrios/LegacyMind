package com.ignacio.legacyanalyzer.domain.ports;

import java.util.List;
import com.ignacio.legacyanalyzer.domain.model.TableDependency;

public interface TableDependencyRepositoryPort {

  void saveAll(List<TableDependency> dependencies);

  List<TableDependency> findBySourceTable(String Table);

}


