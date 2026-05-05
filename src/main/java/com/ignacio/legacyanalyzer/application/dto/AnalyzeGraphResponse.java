package com.ignacio.legacyanalyzer.application.dto;

import java.util.List;
import java.util.Map;

public class AnalyzeGraphResponse {

  private String name;
  private String type;

  private List<String> nodes;
  private List<Map<String, String>> edges;

  private List<String> referencedTables;
  private List<String> codeSmells;

  private Integer riskScore;
  private String riskLevel;
  private String functionalSummary;

  public AnalyzeGraphResponse(String name, String type, List<String> nodes,
      List<Map<String, String>> edges, List<String> referencedTables, List<String> codeSmells,
      Integer riskScore, String riskLevel, String functionalSummary) {
    this.name = name;
    this.type = type;
    this.nodes = nodes;
    this.edges = edges;
    this.referencedTables = referencedTables;
    this.codeSmells = codeSmells;
    this.riskScore = riskScore;
    this.riskLevel = riskLevel;
    this.functionalSummary = functionalSummary;
  }

  public String getName() {
    return name;
  }

  public String getType() {
    return type;
  }

  public List<String> getNodes() {
    return nodes;
  }

  public List<Map<String, String>> getEdges() {
    return edges;
  }

  public List<String> getReferencedTables() {
    return referencedTables;
  }

  public List<String> getCodeSmells() {
    return codeSmells;
  }

  public Integer getRiskScore() {
    return riskScore;
  }

  public String getRiskLevel() {
    return riskLevel;
  }

  public String getFunctionalSummary() {
    return functionalSummary;
  }
}
