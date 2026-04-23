package com.ignacio.legacyanalyzer.domain.model;

import java.util.List;

public class LegacyObject {

    private String id;
    private String name;
    private String type;
    private List<String> procedures;
    private List<String> referencedTables;
    private String sourceCode;
    private List<String> codeSmells;
    private int riskScore;
    private String riskLevel;
    private String functionalSummary;

    public LegacyObject(
            String id,
            String name,
            String type,
            List<String> procedures,
            List<String> referencedTables,
            String sourceCode,
            List<String> codeSmells,
            int riskScore,
            String riskLevel,
            String FunctionalSummary
    ) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.procedures = procedures;
        this.referencedTables = referencedTables;
        this.sourceCode = sourceCode;
        this.codeSmells = codeSmells;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.functionalSummary = FunctionalSummary;
        

    }

    public String getFunctionalSummary() {
        return functionalSummary;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public List<String> getProcedures() {
        return procedures;
    }

    public List<String> getReferencedTables() {
        return referencedTables;
    }

    public String getSourceCode() {
        return sourceCode;
    }
    public List<String> getCodeSmells() {
        return codeSmells;
    }
    public int getRiskScore() {
        return riskScore;
    }
    public String getRiskLevel() {
        return riskLevel;
    }
}