package com.ignacio.legacyanalyzer.infrastructure.adapters.output.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import com.ignacio.legacyanalyzer.domain.model.LegacyObject;
import com.ignacio.legacyanalyzer.domain.ports.LegacyParserPort;

@Component
public class RegexLegacyParserAdapter implements LegacyParserPort {

    @Override
    public LegacyObject parse(String sourceCode) {

        String normalized = sourceCode.toUpperCase();

        // limpiar funciones tipo EXTRACT(...)
        String sql = normalized.replaceAll("\\b[A-Z_]+\\s*\\([^()]*\\)", " ");

        // =============================
        // NAME + TYPE
        // =============================
        String name = null;
        String type = null;

        Matcher nameMatcher = Pattern.compile(
                "CREATE\\s+OR\\s+REPLACE\\s+(PACKAGE|PROCEDURE|FUNCTION)\\s+(BODY\\s+)?(\\w+)",
                Pattern.CASE_INSENSITIVE).matcher(sourceCode);

        if (nameMatcher.find()) {
            type = nameMatcher.group(1).toUpperCase();
            name = nameMatcher.group(3).toUpperCase();
        }

        // =============================
        // PROCEDURES
        // =============================
        List<String> procedures = new ArrayList<>();

        Matcher procMatcher =
                Pattern.compile("\\bPROCEDURE\\s+(\\w+)\\s*(\\(|IS|AS)", Pattern.CASE_INSENSITIVE)
                        .matcher(sourceCode);

        while (procMatcher.find()) {
            String p = procMatcher.group(1).toUpperCase();
            if (name == null || !p.equals(name)) {
                procedures.add(p);
            }
        }

        procedures = procedures.stream().distinct().toList();

        // =============================
        // TABLES
        // =============================
        Set<String> tables = new HashSet<>();

        extractFromTables(sql, tables);
        extractUpdateTables(normalized, tables);
        extractInsertTables(normalized, tables);

        List<String> referencedTables = tables.stream().filter(Objects::nonNull)
                .filter(s -> !s.isBlank()).distinct().toList();

        // =============================
        // RELATIONS
        // =============================
        List<String> relations = extractSemanticRelations(sql);

        // =============================
        // CODE SMELLS
        // =============================
        List<String> smells = new ArrayList<>();

        if (normalized.contains("SELECT *"))
            smells.add("SELECT * detected");
        if (normalized.contains("COMMIT"))
            smells.add("COMMIT inside procedure");
        if (normalized.contains("WHEN OTHERS"))
            smells.add("WHEN OTHERS generic exception handling");

        int score = smells.stream().mapToInt(s -> {
            if (s.contains("SELECT *"))
                return 2;
            if (s.contains("COMMIT"))
                return 2;
            if (s.contains("WHEN OTHERS"))
                return 3;
            return 0;
        }).sum();

        String riskLevel = score <= 2 ? "LOW" : score <= 6 ? "MEDIUM" : "HIGH";

        String summary = "The " + type + " " + name + " interacts with " + referencedTables.size()
                + " tables and has a risk level of " + riskLevel + ".";

        return new LegacyObject(UUID.randomUUID().toString(), name, type, procedures,
                referencedTables, sourceCode, smells, score, riskLevel, summary);
    }

    // =============================
    // SEMANTIC RELATIONS
    // =============================
    public List<String> extractSemanticRelations(String sql) {

        Set<String> relations = new LinkedHashSet<>();

        sql = sql.replace("\n", " ").replace("\r", " ").toUpperCase();

        Pattern updatePattern = Pattern.compile("\\bUPDATE\\s+(\\w+)");
        Pattern insertPattern = Pattern.compile("\\bINSERT\\s+INTO\\s+(\\w+)");
        Pattern fromPattern = Pattern.compile("\\bFROM\\s+([^;]+?)(WHERE|GROUP BY|ORDER BY|$)",
                Pattern.CASE_INSENSITIVE);

        String mainTable = detectMainTable(sql);

        String[] statements = sql.split(";");

        for (String stmt : statements) {

            // =============================
            // UPDATE
            // =============================
            Matcher update = updatePattern.matcher(stmt);
            if (update.find()) {

                String target = clean(update.group(1));

                if (isValidTable(target) && mainTable != null && !target.equals(mainTable)) {
                    relations.add(mainTable + "->" + target);
                }
            }

            // =============================
            // INSERT
            // =============================
            Matcher insert = insertPattern.matcher(stmt);
            if (insert.find()) {

                String target = clean(insert.group(1));

                if (!isValidTable(target))
                    continue;

                Matcher from = fromPattern.matcher(stmt);

                if (from.find()) {
                    String clause = from.group(1);

                    Set<String> sources = extractTables(clause);

                    for (String src : sources) {
                        if (!src.equals(target)) {
                            relations.add(src + "->" + target);
                        }
                    }

                } else {
                    if (mainTable != null && !mainTable.equals(target)) {
                        relations.add(mainTable + "->" + target);
                    }
                }
            }
        }

        System.out.println("RELATIONS >>> " + relations);

        return new ArrayList<>(relations);
    }

    // =============================
    // DETECT MAIN TABLE
    // =============================
    private String detectMainTable(String sql) {

        // 🥇 PRIORIDAD 1 → primer UPDATE (flujo principal)
        Pattern updatePattern = Pattern.compile("\\bUPDATE\\s+(\\w+)", Pattern.CASE_INSENSITIVE);
        Matcher m = updatePattern.matcher(sql);

        if (m.find()) {
            return clean(m.group(1));
        }

        // 🥈 PRIORIDAD 2 → frecuencia (fallback)
        Map<String, Integer> frequency = new HashMap<>();

        Pattern fromPattern = Pattern.compile("\\bFROM\\s+(\\w+)", Pattern.CASE_INSENSITIVE);
        m = fromPattern.matcher(sql);

        while (m.find()) {
            String table = clean(m.group(1));
            frequency.put(table, frequency.getOrDefault(table, 0) + 1);
        }

        return frequency.entrySet().stream().max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse(null);
    }

    // =============================
    // HELPERS
    // =============================
    private Set<String> extractTables(String clause) {

        Set<String> tables = new HashSet<>();

        for (String part : clause.split(",")) {

            String table = part.trim().split(" ")[0];

            if (table.contains(".")) {
                table = table.split("\\.")[1];
            }

            addIfValid(table, tables);
        }

        return tables;
    }

    private void extractFromTables(String sql, Set<String> tables) {

        Matcher matcher =
                Pattern.compile("\\bFROM\\s+([^;]+)", Pattern.CASE_INSENSITIVE).matcher(sql);

        while (matcher.find()) {

            String clause = matcher.group(1);

            clause = clause.split("WHERE")[0];
            clause = clause.split("GROUP BY")[0];
            clause = clause.split("ORDER BY")[0];

            for (String part : clause.split(",")) {

                String table = part.trim().split(" ")[0];

                if (table.contains(".")) {
                    table = table.split("\\.")[1];
                }

                addIfValid(table, tables);
            }
        }
    }

    private void extractUpdateTables(String sql, Set<String> tables) {

        Matcher matcher =
                Pattern.compile("\\bUPDATE\\s+(\\w+)", Pattern.CASE_INSENSITIVE).matcher(sql);

        while (matcher.find()) {
            addIfValid(matcher.group(1), tables);
        }
    }

    private void extractInsertTables(String sql, Set<String> tables) {

        Matcher matcher = Pattern.compile("\\bINSERT\\s+INTO\\s+(\\w+)", Pattern.CASE_INSENSITIVE)
                .matcher(sql);

        while (matcher.find()) {
            addIfValid(matcher.group(1), tables);
        }
    }

    private void addIfValid(String table, Set<String> tables) {

        if (table == null)
            return;

        table = clean(table);

        if (isValidTable(table)) {
            tables.add(table);
        }
    }

    private String clean(String table) {
        return table.replaceAll("[^A-Z0-9_]", "");
    }

    private boolean isValidTable(String table) {
        return table != null && table.matches("[A-Z][A-Z0-9_]*") && table.length() > 2
                && !Set.of("SELECT", "FROM", "WHERE", "INSERT", "INTO", "VALUES", "UPDATE", "SET",
                        "DELETE", "JOIN", "LEFT", "RIGHT", "INNER", "OUTER", "ON", "AND", "OR",
                        "COUNT", "SUM", "AVG", "EXTRACT", "SYSDATE").contains(table);
    }
}
