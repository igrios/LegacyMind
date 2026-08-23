package com.ignacio.legacyanalyzer.infrastructure.adapters.output.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class StructuralObjectExtractor {

    private static final Pattern NAME_PATTERN = Pattern.compile(
            "CREATE\\s+OR\\s+REPLACE\\s+(MATERIALIZED\\s+VIEW|PACKAGE|PROCEDURE|FUNCTION|TRIGGER|VIEW)"
                    + "\\s+(BODY\\s+)?([A-Z][A-Z0-9_.$#@]*)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PROCEDURE_PATTERN = Pattern.compile(
            "\\bPROCEDURE\\s+(\\w+)\\s*(\\(|IS|AS)", Pattern.CASE_INSENSITIVE);

    public Structure extract(String sourceCode) {
        String name = null;
        String type = null;
        Matcher nameMatcher = NAME_PATTERN.matcher(sourceCode);
        if (nameMatcher.find()) {
            type = nameMatcher.group(1).toUpperCase().replaceAll("\\s+", "_");
            name = nameMatcher.group(3).toUpperCase();
        }

        List<String> procedures = new ArrayList<>();
        Matcher procedureMatcher = PROCEDURE_PATTERN.matcher(sourceCode);
        while (procedureMatcher.find()) {
            String procedure = procedureMatcher.group(1).toUpperCase();
            if (name == null || !procedure.equals(name)) procedures.add(procedure);
        }
        return new Structure(name, type, procedures.stream().distinct().toList());
    }

    public record Structure(String name, String type, List<String> procedures) {
    }
}
