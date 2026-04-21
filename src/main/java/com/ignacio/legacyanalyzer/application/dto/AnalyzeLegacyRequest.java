package com.ignacio.legacyanalyzer.application.dto;

public class AnalyzeLegacyRequest {

    private String sourceCode;

    public AnalyzeLegacyRequest() {
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public void setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
    }
}
