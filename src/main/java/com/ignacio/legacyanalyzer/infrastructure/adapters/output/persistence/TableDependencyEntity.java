package com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence;

import jakarta.persistence.Table;
import java.time.LocalDateTime;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

@Entity
@Table(name = "table_dependencies")
public class TableDependencyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String sourceTable;
    private String targetTable;
    private String objectName;

    private LocalDateTime createdAt;

    public TableDependencyEntity() {}

 public TableDependencyEntity(String sourceTable, String targetTable, String objectName) {
        this.sourceTable = sourceTable;
        this.targetTable = targetTable;
        this.objectName = objectName;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }   
    public String getSourceTable() {
        return sourceTable;
    }
    public String getTargetTable() {
        return targetTable;   
    }

    public String getObjectName() {
        return objectName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

public void setId(String id) {
        this.id = id;
    }

    public void setSourceTable(String sourceTable) {
        this.sourceTable = sourceTable;
    }

    public void setTargetTable(String targetTable) {
        this.targetTable = targetTable;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }









}
