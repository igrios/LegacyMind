package com.ignacio.legacyanalyzer.infrastructure.adapters.output.parser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ignacio.legacyanalyzer.domain.model.LegacyObject;
import com.ignacio.legacyanalyzer.domain.ports.LegacyParserPort;

public class RegexLegacyParserAdapter implements LegacyParserPort {

    @Override
    public LegacyObject parse(String sourceCode) {

        String normalizedSourceCode = sourceCode.toUpperCase();

        String objectName = extractObjectName(normalizedSourceCode);
        String objectType = extractObjectType(normalizedSourceCode);
        List<String> procedures = extractProcedures(normalizedSourceCode);
        List<String> referencedTables =
                new ArrayList<>(extractReferencedTables(normalizedSourceCode));

        return new LegacyObject(
                UUID.randomUUID().toString(),
                objectName,
                objectType,
                procedures,
                referencedTables,
                sourceCode
        );
    }

    private String extractObjectName(String sourceCode) {

        Pattern pattern = Pattern.compile(
                "CREATE\\s+OR\\s+REPLACE\\s+(PACKAGE|FUNCTION|PROCEDURE)\\s+([A-Z0-9_]+)"
        );

        Matcher matcher = pattern.matcher(sourceCode);

        if (matcher.find()) {
            return matcher.group(2);
        }

        return "UNKNOWN_OBJECT";
    }

    private String extractObjectType(String sourceCode) {

        if (sourceCode.contains("CREATE OR REPLACE PACKAGE")) {
            return "PACKAGE";
        }

        if (sourceCode.contains("CREATE OR REPLACE FUNCTION")) {
            return "FUNCTION";
        }

        if (sourceCode.contains("CREATE OR REPLACE PROCEDURE")) {
            return "PROCEDURE";
        }

        return "UNKNOWN";
    }

    private List<String> extractProcedures(String source) {

        List<String> procedures = new ArrayList<>();

        Pattern pattern = Pattern.compile(
                "PROCEDURE\\s+([A-Z0-9_]+)"
        );

        Matcher matcher = pattern.matcher(source);

        while (matcher.find()) {
            procedures.add(matcher.group(1));
        }

        return procedures;
    }

    private Set<String> extractReferencedTables(String source) {

        Set<String> tables = new HashSet<>();

        List<String> regexList = List.of(
                "FROM\\s+([A-Z0-9_]+)",
                "JOIN\\s+([A-Z0-9_]+)",
                "INTO\\s+([A-Z0-9_]+)",
                "UPDATE\\s+([A-Z0-9_]+)"
        );

        for (String regex : regexList) {

            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(source);

            while (matcher.find()) {
                tables.add(matcher.group(1));
            }
        }

        return tables;
    }
}