package com.ignacio.legacyanalyzer.domain.model;

public class TableReference {

  private final String fullName;
  private final String alias;

  public TableReference(String fullName, String alias) {
    this.fullName = fullName;
    this.alias = alias;
  }

  public String getFullName() {
    return fullName;
  }       

  public String getAlias() {
    return alias;
  }

  @Override
public String toString() {
    return alias != null ?
           fullName + " AS " + alias :
           fullName;  
  }


}
