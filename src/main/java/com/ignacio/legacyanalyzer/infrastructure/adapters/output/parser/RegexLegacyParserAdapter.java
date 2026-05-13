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
import com.ignacio.legacyanalyzer.domain.model.KnowledgeRelation;
import com.ignacio.legacyanalyzer.domain.model.LegacyObject;
import com.ignacio.legacyanalyzer.domain.model.SubprogramNode;
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
        // SUBPROGRAMS
        // =============================
        SubprogramExtractor subprogramExtractor = new SubprogramExtractor();

        List<SubprogramNode> subprograms = subprogramExtractor.extract(sourceCode, name);

        System.out.println("SUBPROGRAMS >>> " + subprograms.size());

        System.out.println("FROM CLAUSE >>> " + extractTopLevelFromClause(sql));

        // =============================
        // READ TABLES
        // =============================

        List<String> readTables = extractReadTables(sourceCode.toUpperCase());

        System.out.println("READ TABLES FINAL >>> " + readTables);

        // =============================
        // WRITE TABLES
        // =============================

        List<String> writeTables = extractWriteTables(sourceCode.toUpperCase());

        System.out.println("WRITE TABLES FINAL >>> " + writeTables);

        // =============================
        // REFERENCED TABLES
        // =============================

        Set<String> referencedTablesSet = new LinkedHashSet<>();

        referencedTablesSet.addAll(readTables);

        referencedTablesSet.addAll(writeTables);

        List<String> referencedTables = referencedTablesSet.stream().filter(Objects::nonNull)
                .filter(s -> !s.isBlank()).distinct().toList();

        // =============================
        // RELATIONS
        // =============================
        List<String> relations = extractSemanticRelations(sql);

        // =============================
        // KNOWLEDGE RELATIONS
        // =============================
        List<KnowledgeRelation> knowledgeRelations = extractKnowledgeRelations(sourceCode, name);

        // =============================
        // CODE SMELLS
        // =============================
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

        int score = smells.stream().mapToInt(s -> {

            if (s.contains("SELECT *")) {
                return 2;
            }

            if (s.contains("COMMIT")) {
                return 2;
            }

            if (s.contains("WHEN OTHERS")) {
                return 3;
            }

            return 0;
        }).sum();

        String riskLevel = score <= 2 ? "LOW" : score <= 6 ? "MEDIUM" : "HIGH";

        String summary = "The " + type + " " + name + " interacts with " + referencedTables.size()
                + " tables and has a risk level of " + riskLevel + ".";

        System.out.println("REFERENCED TABLES BEFORE OBJECT >>> " + referencedTables);
        return new LegacyObject(

                UUID.randomUUID().toString(),

                name,

                type,

                procedures,

                referencedTables,

                subprograms,

                knowledgeRelations,

                sourceCode,

                smells,

                score,

                riskLevel,

                summary);
    }

    // =============================
    // SEMANTIC RELATIONS
    // =============================
    public List<String> extractSemanticRelations(String sql) {

        System.out.println("NEW SEMANTIC RELATIONS RUNNING");

        Set<String> relations = new LinkedHashSet<>();

        sql = sql.replace("\n", " ").replace("\r", " ").toUpperCase();

        Pattern updatePattern = Pattern.compile("\\bUPDATE\\s+(\\w+)", Pattern.CASE_INSENSITIVE);

        Pattern insertPattern =
                Pattern.compile("\\bINSERT\\s+INTO\\s+(\\w+)", Pattern.CASE_INSENSITIVE);

        String mainTable = detectMainTable(sql);

        String[] statements = sql.split(";");

        for (String stmt : statements) {

            System.out.println("STATEMENT >>> " + stmt);

            // =============================
            // UPDATE RELATIONS
            // =============================

            Matcher update = updatePattern.matcher(stmt);

            if (update.find()) {

                String target = clean(update.group(1));

                if (isValidTable(target) && mainTable != null && !target.equals(mainTable)) {

                    relations.add(mainTable + "->" + target);
                }
            }

            // =============================
            // INSERT RELATIONS
            // =============================

            Matcher insert = insertPattern.matcher(stmt);

            if (insert.find()) {

                String target = clean(insert.group(1));

                if (isValidTable(target)) {

                    String fromClause = extractTopLevelFromClause(stmt);

                    System.out.println("FROM FOR INSERT >>> " + fromClause);

                    Set<String> sources = extractTables(fromClause);

                    System.out.println("INSERT SOURCES >>> " + sources);

                    for (String src : sources) {

                        if (!src.equals(target)) {

                            relations.add(src + "->" + target);
                        }
                    }
                }
            }

            // =============================
            // SELECT / IMPLICIT JOIN RELATIONS
            // =============================

            String fromClause = extractTopLevelFromClause(stmt);

            System.out.println("FROM FOR REL >>> " + fromClause);

            if (fromClause == null || fromClause.isBlank()) {
                continue;
            }

            Set<String> sources = extractTables(fromClause);

            System.out.println("SOURCES >>> " + sources);

            List<String> sourceList = new ArrayList<>(sources);

            for (int i = 0; i < sourceList.size(); i++) {

                for (int j = i + 1; j < sourceList.size(); j++) {

                    String left = sourceList.get(i);

                    String right = sourceList.get(j);

                    if (isValidTable(left) && isValidTable(right) && !left.equals(right)) {

                        relations.add(left + "->" + right);
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

        Pattern updatePattern = Pattern.compile("\\bUPDATE\\s+(\\w+)", Pattern.CASE_INSENSITIVE);

        Matcher m = updatePattern.matcher(sql);

        if (m.find()) {
            return clean(m.group(1));
        }

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

        if (clause == null || clause.isBlank()) {
            return tables;
        }

        List<String> parts = splitTopLevelComma(clause);

        for (String part : parts) {

            System.out.println("PART >>> " + part);

            String trimmed = part.trim();

            // =============================
            // SUBQUERY
            // =============================

            if (trimmed.startsWith("(")) {

                List<String> nestedTables = extractReadTables(trimmed);

                System.out.println("NESTED TABLES >>> " + nestedTables);

                tables.addAll(nestedTables);

                continue;
            }

            // =============================
            // NORMAL TABLE
            // =============================

            String table = trimmed.split("\\s+")[0];

            if (table.contains(".")) {

                table = table.substring(table.lastIndexOf(".") + 1);
            }

            table = clean(table);

            // 🔥 IMPORTANTE
            // NO uses isValidTable acá todavía

            if (table != null && !table.isBlank()) {

                tables.add(table);

                System.out.println("EXTRACT TABLE >>> " + table);
            }
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

        if (table == null) {
            return;
        }

        table = clean(table);

        if (isValidTable(table)) {
            tables.add(table);
        }
    }

    // INICIO MOD
    private String extractTopLevelFromClause(String sql) {

        String normalized = sql.replaceAll("\\s+", " ");

        int fromIndex = normalized.toUpperCase().indexOf(" FROM ");

        if (fromIndex == -1) {
            return "";
        }

        int depth = 0;

        StringBuilder fromClause = new StringBuilder();

        for (int i = fromIndex + 6; i < normalized.length(); i++) {

            char current = normalized.charAt(i);

            if (current == '(') {
                depth++;
            }

            if (current == ')') {
                depth--;
            }

            String remaining = normalized.substring(i).toUpperCase();

            if (depth == 0 && (remaining.startsWith(" WHERE ") || remaining.startsWith(" GROUP BY ")
                    || remaining.startsWith(" ORDER BY ")
                    || remaining.startsWith(" CONNECT BY "))) {
                break;
            }

            fromClause.append(current);
        }

        return fromClause.toString().trim();


    }

    //////
    ///
    ///
    private List<String> splitTopLevelComma(String text) {

        List<String> result = new ArrayList<>();

        StringBuilder current = new StringBuilder();

        int depth = 0;

        for (int i = 0; i < text.length(); i++) {

            char c = text.charAt(i);

            if (c == '(') {
                depth++;
            }

            if (c == ')') {
                depth--;
            }

            if (c == ',' && depth == 0) {

                result.add(current.toString().trim());

                current.setLength(0);

                continue;
            }

            current.append(c);
        }

        if (!current.isEmpty()) {

            result.add(current.toString().trim());
        }

        return result;
    }

    // FIN MOD



    private String clean(String table) {

        return table.replaceAll("[^A-Z0-9_]", "");
    }

    private boolean isValidTable(String table) {

        return table != null && !table.isBlank() && table.matches("[A-Z][A-Z0-9_]*")
                && !Set.of("SELECT", "FROM", "WHERE", "INSERT", "INTO", "VALUES", "UPDATE", "SET",
                        "DELETE", "JOIN", "LEFT", "RIGHT", "INNER", "OUTER", "ON", "AND", "OR",
                        "GROUP", "ORDER", "BY", "BEGIN", "END", "NULL").contains(table);
    }

    private List<KnowledgeRelation> extractKnowledgeRelations(String sourceCode,
            String objectName) {

        List<KnowledgeRelation> relations = new ArrayList<>();

        // =============================
        // CALLS
        // =============================

        Pattern callPattern = Pattern.compile("(\\w+)\\.(\\w+)\\s*\\(", Pattern.CASE_INSENSITIVE);

        Matcher matcher = callPattern.matcher(sourceCode);

        while (matcher.find()) {

            String packageName = matcher.group(1).toUpperCase();

            String procedureName = matcher.group(2).toUpperCase();

            // evitar self calls
            if (packageName.equals(objectName)) {
                continue;
            }

            relations.add(

                    new KnowledgeRelation(

                            objectName,

                            "CALLS",

                            packageName));

            System.out.println(

                    "CALL DETECTED >>> " + objectName + " -> " + packageName + "." + procedureName);
        }

        // =============================
        // READS
        // =============================

        List<String> readTables = extractReadTables(sourceCode.toUpperCase());

        System.out.println("READ TABLES FINAL >>> " + readTables);

        for (String table : readTables) {

            relations.add(

                    new KnowledgeRelation(

                            objectName,

                            "READS",

                            table));

            System.out.println(

                    "READ DETECTED >>> " + objectName + " -> " + table);
        }

        // =============================
        // WRITES
        // =============================

        List<String> writeTables = extractWriteTables(sourceCode.toUpperCase());

        for (String table : writeTables) {

            relations.add(

                    new KnowledgeRelation(

                            objectName,

                            "WRITES",

                            table));

            System.out.println("WRITE DETECTED >>> " + objectName + " -> " + table);
        }

        return relations.stream().distinct().toList();
    }

    private List<String> extractReadTables(String sourceCode) {

        Set<String> tables = new HashSet<>();

        String normalized = sourceCode.toUpperCase();

        int index = 0;

        while ((index = normalized.indexOf(" FROM ", index)) != -1) {

            String remaining = normalized.substring(index);

            String fromClause = extractTopLevelFromClause(remaining);

            System.out.println("FROM CLAUSE >>> " + fromClause);

            List<String> parts = splitTopLevelComma(fromClause);

            for (String part : parts) {

                System.out.println("PART >>> " + part);

                String trimmed = part.trim();

                // ignorar subquery completa
                if (trimmed.startsWith("(")) {

                    // recursion sobre subquery
                    tables.addAll(extractReadTables(trimmed));

                    continue;
                }

                String table = trimmed.split("\\s+")[0];

                table = clean(table);

                if (isValidTable(table)) {

                    tables.add(table);

                    System.out.println("READ TABLE >>> " + table);
                }
            }

            index += 6;
        }

        return tables.stream().distinct().toList();
    }

    private List<String> extractWriteTables(String sourceCode) {

        List<String> result = new ArrayList<>();

        // UPDATE

        Pattern updatePattern =
                Pattern.compile("\\bUPDATE\\s+([A-Z0-9_]+)", Pattern.CASE_INSENSITIVE);

        Matcher updateMatcher = updatePattern.matcher(sourceCode);

        while (updateMatcher.find()) {

            String table = updateMatcher.group(1).toUpperCase();

            if (isValidTable(table)) {

                result.add(table);
            }
        }

        // INSERT INTO

        Pattern insertPattern =
                Pattern.compile("\\bINSERT\\s+INTO\\s+([A-Z0-9_]+)", Pattern.CASE_INSENSITIVE);

        Matcher insertMatcher = insertPattern.matcher(sourceCode);

        while (insertMatcher.find()) {

            String table = insertMatcher.group(1).toUpperCase();

            if (isValidTable(table)) {

                result.add(table);
            }
        }

        return result.stream().distinct().toList();
    }


}
