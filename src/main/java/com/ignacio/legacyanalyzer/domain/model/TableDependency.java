package com.ignacio.legacyanalyzer.domain.model;

public class TableDependency {

  private String sourceTable;
  private String targetTable;
  private String objectName;


  public TableDependency(String sourceTable, String targetTable, String objectName) {
    this.sourceTable = sourceTable;
    this.targetTable = targetTable;
    this.objectName = objectName;
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

}
