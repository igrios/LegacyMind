package com.ignacio.legacyanalyzer.domain.model;

import java.util.ArrayList;
import java.util.List;

public class SqlSemanticModel {

    private String originalSql;

    private List<String> readTables = new ArrayList<>();

    private List<String> writeTables = new ArrayList<>();

    private List<TableReference> tableReferences = new ArrayList<>();

    private List<JoinCondition> joinConditions = new ArrayList<>();

    private List<String> semanticRelations = new ArrayList<>();

    private List<RiskFinding> findings = new ArrayList<>();

    private String normalizedSql;

    private int riskScore;

    private String riskLevel;

    public List<String> getReadTables() {
        return readTables;
    }

    public void setReadTables(List<String> readTables) {
        this.readTables = readTables;
    }

    public List<String> getWriteTables() {
        return writeTables;
    }

    public void setWriteTables(List<String> writeTables) {
        this.writeTables = writeTables;
    }

    public List<TableReference> getTableReferences() {
        return tableReferences;
    }

    public void setTableReferences(List<TableReference> tableReferences) {
        this.tableReferences = tableReferences;
    }

    public List<JoinCondition> getJoinConditions() {
        return joinConditions;
    }

    public void setJoinConditions(List<JoinCondition> joinConditions) {
        this.joinConditions = joinConditions;
    }

    public List<String> getSemanticRelations() {
        return semanticRelations;
    }

    public void setSemanticRelations(List<String> semanticRelations) {
        this.semanticRelations = semanticRelations;
    }

    public List<RiskFinding> getFindings() {
        return findings;
    }

    public void setFindings(List<RiskFinding> findings) {

        this.findings = findings;
    }

    public String getOriginalSql() {
        return originalSql;
    }

    public void setOriginalSql(String originalSql) {
        this.originalSql = originalSql;
    }

    public String getNormalizedSql() {
        return normalizedSql;
    }

    public void setNormalizedSql(String normalizedSql) {
        this.normalizedSql = normalizedSql;
    }

    public int getRiskScore() {
        return riskScore;
    }
    public void setRiskScore(int riskScore) {
        this.riskScore = riskScore;
    }

    public String getRiskLevel() {
        return riskLevel;
    }
    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }
    

    @Override
    public String toString() {

        return """
                SqlSemanticModel{
                    readTables=%s,
                    writeTables=%s,
                    tableReferences=%s,
                    joinConditions=%s,
                    semanticRelations=%s,
                    findings=%s,
                    originalSql=%s
                }
                """.formatted(readTables, writeTables, tableReferences, joinConditions,
                semanticRelations, findings, originalSql);
    }
}
