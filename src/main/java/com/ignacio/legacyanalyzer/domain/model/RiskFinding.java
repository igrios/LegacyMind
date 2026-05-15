package com.ignacio.legacyanalyzer.domain.model;

public class RiskFinding {

    private final String severity;

    private final String type;

    private final String message;

    public RiskFinding(
            String severity,
            String type,
            String message) {

        this.severity = severity;
        this.type = type;
        this.message = message;
    }

    public String getSeverity() {
        return severity;
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {

        return severity
                + " | "
                + type
                + " | "
                + message;
    }
}