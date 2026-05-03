package com.ignacio.legacyanalyzer.infrastructure.adapters.output.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.ignacio.legacyanalyzer.domain.model.LegacyObject;
import com.ignacio.legacyanalyzer.domain.ports.LegacyParserPort;
import org.springframework.stereotype.Component;

@Component
public class RegexLegacyParserAdapter implements LegacyParserPort {

    @Override
    public LegacyObject parse(String sourceCode) {

        String normalized = sourceCode.toUpperCase();

        // ----------------------------
        // NAME + TYPE
        // ----------------------------
        // ----------------------------
        // NAME + TYPE
        // ----------------------------
        String name = null;
        String type = null;
        boolean isBody = false;

        Pattern namePattern = Pattern.compile(
                "CREATE\\s+OR\\s+REPLACE\\s+(PACKAGE|PROCEDURE|FUNCTION)\\s+(BODY\\s+)?(\\w+)",
                Pattern.CASE_INSENSITIVE);

        Matcher nameMatcher = namePattern.matcher(sourceCode);

        if (nameMatcher.find()) {
            type = nameMatcher.group(1).toUpperCase();
            isBody = nameMatcher.group(2) != null;
            name = nameMatcher.group(3).toUpperCase();
        }

        // ----------------------------
        // PROCEDURES (FIX REAL)
        // ----------------------------
        Pattern procPattern =
                Pattern.compile("\\bPROCEDURE\\s+(\\w+)\\s*(\\(|IS|AS)", Pattern.CASE_INSENSITIVE);

        Matcher procMatcher = procPattern.matcher(sourceCode);

        List<String> procedures = new ArrayList<>();

        while (procMatcher.find()) {
            String procName = procMatcher.group(1).toUpperCase();

            // evitar falsos positivos (ej: nombre del package)
            if (name == null || !procName.equalsIgnoreCase(name)) {
                procedures.add(procName);
            }
        }

        // eliminar duplicados
        procedures = procedures.stream().distinct().toList();

        // ----------------------------
        // TABLES + ALIAS (IMPLICIT JOIN SUPPORT)
        // ----------------------------
        Map<String, String> aliasMap = extractTablesWithAlias(sourceCode);
        Set<String> tables = new HashSet<>(aliasMap.values());

        // ----------------------------
        // EXPLICIT JOIN SUPPORT
        // ----------------------------
        Pattern joinPattern = Pattern.compile("(FROM|JOIN)\\s+(\\w+)", Pattern.CASE_INSENSITIVE);

        Matcher joinMatcher = joinPattern.matcher(sourceCode);

        while (joinMatcher.find()) {
            tables.add(joinMatcher.group(2).toUpperCase());
        }

        // ----------------------------
        // UPDATE SUPPORT
        // ----------------------------
        Pattern updatePattern = Pattern.compile("UPDATE\\s+(\\w+)", Pattern.CASE_INSENSITIVE);
        Matcher updateMatcher = updatePattern.matcher(sourceCode);

        while (updateMatcher.find()) {
            tables.add(updateMatcher.group(1).toUpperCase());
        }

        // ----------------------------
        // INSERT SUPPORT (🔥 FIX)
        // ----------------------------
        Pattern insertPattern =
                Pattern.compile("INSERT\\s+INTO\\s+(\\w+)", Pattern.CASE_INSENSITIVE);
        Matcher insertMatcher = insertPattern.matcher(sourceCode);

        while (insertMatcher.find()) {
            tables.add(insertMatcher.group(1).toUpperCase());
        }

        // ----------------------------
        // CLEAN TABLE LIST (SIEMPRE AL FINAL)
        // ----------------------------
        List<String> referencedTables =
                tables.stream().filter(t -> t != null && !t.isBlank()).distinct().toList();

        // ----------------------------
        // RELACIONES IMPLÍCITAS (base para dependency graph)
        // ----------------------------
        List<String> relations = extractImplicitRelations(sourceCode, aliasMap);
        // (todavía no las guardamos en el modelo, pero ya están listas)

        // ----------------------------
        // CODE SMELLS
        // ----------------------------
        List<String> smells = new ArrayList<>();

        if (normalized.contains("SELECT *")) {
            smells.add("SELECT * detected");
        }

        if (normalized.contains("COMMIT")) {
            smells.add("COMMIT inside procedure");
        }

        if (normalized.contains("WHEN OTHERS")) {
            smells.add("WHEN OTHERS generic exception handling");
        }

        if (normalized.contains("EXECUTE IMMEDIATE")) {
            smells.add("Dynamic SQL detected (EXECUTE IMMEDIATE)");
        }
        smells = smells.stream()
    .filter(s -> s != null && !s.trim().isEmpty())
    .distinct()
    .toList();
        // ----------------------------
        // RISK SCORE
        // ----------------------------
        int score = 0;

        for (String smell : smells) {
            if (smell.contains("SELECT *"))
                score += 2;
            if (smell.contains("COMMIT"))
                score += 2;
            if (smell.contains("WHEN OTHERS"))
                score += 3;
            if (smell.contains("EXECUTE IMMEDIATE"))
                score += 4;
        }

        String riskLevel;
        if (score <= 2)
            riskLevel = "LOW";
        else if (score <= 6)
            riskLevel = "MEDIUM";
        else
            riskLevel = "HIGH";

        // ----------------------------
        // FUNCTIONAL SUMMARY
        // ----------------------------
        String summary = "The " + type + " " + name + " interacts with " + referencedTables.size()
                + " tables and has a risk level of " + riskLevel + ".";

        // ----------------------------
        // 🔥 RETURN INMUTABLE OBJECT
        // ----------------------------
        return new LegacyObject(UUID.randomUUID().toString(), name, type, procedures,
                referencedTables, sourceCode, smells, score, riskLevel, summary);
    }

    // ======================================================
    // 🔥 TABLE + ALIAS EXTRACTION (IMPLICIT JOIN)
    // ======================================================
    private Map<String, String> extractTablesWithAlias(String sql) {

        Map<String, String> aliasToTable = new HashMap<>();

        Pattern fromPattern = Pattern.compile("FROM\\s+(.*?)\\s+(WHERE|GROUP BY|ORDER BY|$)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

        Matcher matcher = fromPattern.matcher(sql);

        if (matcher.find()) {
            String fromClause = matcher.group(1);

            String[] tables = fromClause.split(",");

            for (String table : tables) {

                String cleaned = table.trim().replaceAll("\\s+", " ");

                if (cleaned.isBlank())
                    continue;

                String[] parts = cleaned.split(" ");

                String tableName = parts[0].toUpperCase();

                if (parts.length == 1) {
                    aliasToTable.put(tableName, tableName);
                } else {
                    String alias = parts[1].toUpperCase();
                    aliasToTable.put(alias, tableName);
                }
            }
        }


        return aliasToTable;
    }

    // ======================================================
    // 🔥 IMPLICIT JOIN RELATION DETECTION
    // ======================================================
    private List<String> extractImplicitRelations(String sql, Map<String, String> aliasMap) {

        List<String> relations = new ArrayList<>();

        Pattern wherePattern = Pattern.compile("WHERE\\s+(.*?)(GROUP BY|ORDER BY|$)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

        Matcher matcher = wherePattern.matcher(sql);

        if (matcher.find()) {

            String whereClause = matcher.group(1);

            Pattern relationPattern = Pattern.compile("(\\w+)\\.(\\w+)\\s*=\\s*(\\w+)\\.(\\w+)",
                    Pattern.CASE_INSENSITIVE);

            Matcher relMatcher = relationPattern.matcher(whereClause);

            while (relMatcher.find()) {

                String leftAlias = relMatcher.group(1).toUpperCase();
                String rightAlias = relMatcher.group(3).toUpperCase();

                String leftTable = aliasMap.getOrDefault(leftAlias, leftAlias);
                String rightTable = aliasMap.getOrDefault(rightAlias, rightAlias);

                relations.add(leftTable + " -> " + rightTable);
            }
        }

        return relations;
    }
}
