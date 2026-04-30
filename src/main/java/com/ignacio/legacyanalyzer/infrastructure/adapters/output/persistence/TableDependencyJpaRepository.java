package com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TableDependencyJpaRepository
        extends JpaRepository<TableDependencyEntity, String> {

    List<TableDependencyEntity> findBySourceTable(String sourceTable);

    List<TableDependencyEntity> findByTargetTable(String targetTable);
}