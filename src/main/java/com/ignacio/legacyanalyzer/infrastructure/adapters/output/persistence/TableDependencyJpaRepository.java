package com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TableDependencyJpaRepository
        extends JpaRepository<TableDependencyEntity, Long> {

    List<TableDependencyEntity> findBySourceTable(String sourceTable);

    List<TableDependencyEntity> findByTargetTable(String targetTable);


    


}