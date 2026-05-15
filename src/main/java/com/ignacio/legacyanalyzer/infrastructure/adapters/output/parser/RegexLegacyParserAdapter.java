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
import com.ignacio.legacyanalyzer.domain.model.JoinCondition;
import com.ignacio.legacyanalyzer.domain.model.KnowledgeRelation;
import com.ignacio.legacyanalyzer.domain.model.LegacyObject;
import com.ignacio.legacyanalyzer.domain.model.SqlSemanticModel;
import com.ignacio.legacyanalyzer.domain.model.SubprogramNode;
import com.ignacio.legacyanalyzer.domain.model.TableReference;
import com.ignacio.legacyanalyzer.domain.ports.LegacyParserPort;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
public class RegexLegacyParserAdapter implements LegacyParserPort {

    @Override
    public LegacyObject parse(String sourceCode) {

        String cleanSource = preProcess(sourceCode);

        String normalized = cleanSource.toUpperCase();


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

       
        log.debug("SUBPROGRAMS >>> {}", subprograms.size());     
      
       log.debug("FROM CLAUSE >>> " + extractTopLevelFromClause(sql));



        // =============================
        // READ TABLES
        // =============================

        List<String> readTables = extractReadTables(normalized);

        log.debug("READ TABLES FINAL >>> {}", readTables);

        // =============================
        // WRITE TABLES
        // =============================

        List<String> writeTables = extractWriteTables(normalized);

        log.debug("WRITE TABLES FINAL >>> {}", writeTables);

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
        List<String> relations = extractSemanticRelations(normalized);

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

        log.debug("REFERENCED TABLES BEFORE OBJECT >>> {}", referencedTables);
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

        log.debug("NEW SEMANTIC RELATIONS RUNNING");

        Set<String> relations = new LinkedHashSet<>();

        sql = sql.replace("\n", " ").replace("\r", " ").toUpperCase();

        Pattern updatePattern = Pattern.compile(
                "\\bUPDATE\\s+([A-Z][A-Z0-9_]*(?:\\.[A-Z][A-Z0-9_]*)?)", Pattern.CASE_INSENSITIVE);

        Pattern insertPattern =
                Pattern.compile("\\bINSERT\\s+INTO\\s+([A-Z][A-Z0-9_]*(?:\\.[A-Z][A-Z0-9_]*)?)",
                        Pattern.CASE_INSENSITIVE);

        String mainTable = detectMainTable(sql);

        String[] statements = sql.split(";");

        for (String stmt : statements) {

            log.debug("STATEMENT >>> {}", stmt);

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

                    log.debug("FROM FOR INSERT >>> {}", fromClause);

                    Set<String> sources = extractTables(fromClause);

                    log.debug("INSERT SOURCES >>> {}", sources);

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

            log.debug("FROM FOR REL >>> {}", fromClause);

            if (fromClause == null || fromClause.isBlank()) {
                continue;
            }

            Set<String> sources = extractTables(fromClause);

            log.debug("SOURCES >>> {}", sources);

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

     log.debug("RELATIONS >>> {}", relations);

        return new ArrayList<>(relations);
    }

    // =============================
    // DETECT MAIN TABLE
    // =============================
    private String detectMainTable(String normalized) {

        // PRIORIDAD 1:
        // si existe UPDATE, esa suele ser la tabla principal

        Pattern updatePattern = Pattern.compile("\\bUPDATE\\s+(\\w+)", Pattern.CASE_INSENSITIVE);

        Matcher updateMatcher = updatePattern.matcher(normalized);

        if (updateMatcher.find()) {

            String table = clean(updateMatcher.group(1));

            if (isValidTable(table)) {
                return table;
            }
        }

        // PRIORIDAD 2:
        // inferir por frecuencia de lectura

        Map<String, Integer> frequency = new HashMap<>();

        for (String table : extractReadTables(normalized)) {

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

            log.debug("PART >>> {}", part);

            String trimmed = part.trim();

            // =============================
            // SUBQUERY
            // =============================

            if (trimmed.startsWith("(")) {

                List<String> nestedTables = extractReadTables(trimmed);

                log.debug("NESTED TABLES >>> {}", nestedTables);

                tables.addAll(nestedTables);

                continue;
            }

            // =============================
            // NORMAL TABLE
            // =============================

            String table = trimmed.split("\\s+")[0];

            // if (table.contains(".")) {

            // table = table.substring(table.lastIndexOf(".") + 1);
            // }

            table = clean(table);

            // 🔥 IMPORTANTE
            // NO uses isValidTable acá todavía

            if (table != null && !table.isBlank()) {

                tables.add(table);

                log.debug("EXTRACT TABLE >>> {}", table);
            }
        }

        Pattern joinPattern =
                Pattern.compile("\\bJOIN\\s+([A-Z][A-Z0-9_.$#@]*(?:\\.[A-Z][A-Z0-9_.$#@]*)?)",
                        Pattern.CASE_INSENSITIVE);

        Matcher joinMatcher = joinPattern.matcher(clause);

        while (joinMatcher.find()) {

            String table = clean(joinMatcher.group(1));

            if (table != null && !table.isBlank()) {

                tables.add(table);

                log.debug("JOIN EXTRACT TABLE >>> {}", table);
            }
        }

        return tables;
    }


    public List<TableReference> extractTableReferences(String clause) {

        List<TableReference> references = new ArrayList<>();

        if (clause == null || clause.isBlank()) {
            return references;
        }

        List<String> parts = splitTopLevelComma(clause);

        for (String part : parts) {

            String trimmed = part.trim();

            if (trimmed.startsWith("(")) {
                continue;
            }

            String[] tokens = trimmed.split("\\s+");

            if (tokens.length == 0) {
                continue;
            }

            String table = clean(tokens[0]);

            String alias = null;

            if (tokens.length > 1) {

                String possibleAlias = tokens[1].toUpperCase();

                if (!possibleAlias.equals("INNER") && !possibleAlias.equals("LEFT")
                        && !possibleAlias.equals("RIGHT") && !possibleAlias.equals("JOIN")
                        && !possibleAlias.equals("ON")) {

                    alias = possibleAlias;
                }
            }

            references.add(new TableReference(table, alias));

            log.debug("TABLE REF >>> {} alias={}", table, alias);
        }


        Pattern joinPattern = Pattern.compile(
                "\\bJOIN\\s+([A-Z][A-Z0-9_.$#@]*(?:\\.[A-Z][A-Z0-9_.$#@]*)?)\\s+([A-Z][A-Z0-9_]*)",
                Pattern.CASE_INSENSITIVE);

        Matcher joinMatcher = joinPattern.matcher(clause);

        while (joinMatcher.find()) {

            String table = clean(joinMatcher.group(1));

            String alias = joinMatcher.group(2);

            references.add(new TableReference(table, alias));

            log.debug("JOIN TABLE REF >>> {} alias={}", table, alias);
        }


        return references;
    }


    public List<JoinCondition> extractJoinConditions(String sql) {

        List<JoinCondition> conditions = new ArrayList<>();

        Pattern joinPattern = Pattern.compile(
                "([A-Z][A-Z0-9_]*)\\.([A-Z][A-Z0-9_]*)\\s*=\\s*([A-Z][A-Z0-9_]*)\\.([A-Z][A-Z0-9_]*)",
                Pattern.CASE_INSENSITIVE);

        Matcher matcher = joinPattern.matcher(sql.toUpperCase());

        while (matcher.find()) {

            String leftAlias = matcher.group(1);

            String leftColumn = matcher.group(2);

            String rightAlias = matcher.group(3);

            String rightColumn = matcher.group(4);

            JoinCondition condition =
                    new JoinCondition(leftAlias, leftColumn, rightAlias, rightColumn);

            conditions.add(condition);

            log.debug("JOIN CONDITION >>> {}", condition);
        }

        return conditions;
    }

    public void resolveJoinConditions(List<TableReference> references,
            List<JoinCondition> conditions) {

        Map<String, String> aliasMap = new HashMap<>();

        for (TableReference ref : references) {

            if (ref.getAlias() != null) {

                aliasMap.put(ref.getAlias().toUpperCase(), ref.getFullName());
            }
        }

        log.debug("ALIAS MAP >>> {}", aliasMap);

        for (JoinCondition condition : conditions) {

            String leftTable = aliasMap.get(condition.getLeftAlias());

            String rightTable = aliasMap.get(condition.getRightAlias());

            log.debug("RESOLVED JOIN >>> {}.{} -> {}.{}", leftTable, condition.getLeftColumn(),
                    rightTable, condition.getRightColumn());
        }
    }

    public SqlSemanticModel buildSemanticModel(String sql) {

        SqlSemanticModel model = new SqlSemanticModel();

        List<String> readTables = extractReadTables(sql);

        List<String> writeTables = extractWriteTables(sql);

        List<String> semanticRelations = extractSemanticRelations(sql);

        String fromClause = extractTopLevelFromClause(sql.toUpperCase());

        List<TableReference> references = extractTableReferences(fromClause);

        List<JoinCondition> joins = extractJoinConditions(sql);

        model.setReadTables(readTables);

        model.setWriteTables(writeTables);

        model.setSemanticRelations(semanticRelations);

        model.setTableReferences(references);

        model.setJoinConditions(joins);

        return model;
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
    //
    public String extractTopLevelFromClause(String sql) {

        String normalized = sql.replaceAll("\\s+", " ").trim();

        Pattern fromPattern = Pattern.compile("\\bFROM\\b", Pattern.CASE_INSENSITIVE);

        Matcher matcher = fromPattern.matcher(normalized);

        if (!matcher.find()) {
            return "";
        }

        int fromIndex = matcher.end();

        int depth = 0;

        StringBuilder fromClause = new StringBuilder();

        for (int i = fromIndex; i < normalized.length(); i++) {

            char current = normalized.charAt(i);

            if (current == '(') {
                depth++;
            }

            if (current == ')') {
                depth--;
            }

            if (depth == 0) {

                String remaining = normalized.substring(i).toUpperCase();

                if (remaining.startsWith(" WHERE ") || remaining.startsWith(" GROUP BY ")
                        || remaining.startsWith(" ORDER BY ")
                        || remaining.startsWith(" CONNECT BY ") || remaining.startsWith(" HAVING ")
                        || remaining.startsWith(" UNION ") || remaining.startsWith(" INTERSECT ")
                        || remaining.startsWith(" MINUS ") || remaining.startsWith(";")) {

                    break;
                }
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

    // =============================
    // PRE PROCESS
    // =============================
    private String preProcess(String source) {

        // remove block comments
        source = source.replaceAll("/\\*.*?\\*/", " ");

        // remove line comments
        source = source.replaceAll("--.*?(\\r?\\n)", " ");

        // replace string literals
        source = source.replaceAll("'(?:''|[^'])*'", "''");

        return source;
    }



    private String clean(String table) {

        return table.replaceAll("[^A-Z0-9_.$#@]", "");
    }

    private boolean isValidTable(String table) {

        return table != null && !table.isBlank() && table.matches("[A-Z][A-Z0-9_.$#@]*")
                && !Set.of("SELECT", "FROM", "WHERE", "INSERT", "INTO", "VALUES", "UPDATE", "SET",
                        "DELETE", "JOIN", "LEFT", "RIGHT", "INNER", "OUTER", "ON", "AND", "OR",
                        "GROUP", "ORDER", "BY", "BEGIN", "END", "NULL").contains(table);
    }

    private List<KnowledgeRelation> extractKnowledgeRelations(String sourceCode,
            String objectName) {

        String normalized = preProcess(sourceCode).toUpperCase();

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

          log.debug(
        "CALL DETECTED >>> {} -> {}.{}",
        objectName,
        packageName,
        procedureName);
        }

        // =============================
        // READS
        // =============================

        List<String> readTables = extractReadTables(normalized);

        log.debug("READ TABLES FINAL >>> {}", readTables);

        for (String table : readTables) {

            relations.add(

                    new KnowledgeRelation(

                            objectName,

                            "READS",

                            table));

            log.debug("READ DETECTED >>> {} -> {}", objectName, table);
        }

        // =============================
        // WRITES
        // =============================

        List<String> writeTables = extractWriteTables(normalized);

        for (String table : writeTables) {

            relations.add(

                    new KnowledgeRelation(

                            objectName,

                            "WRITES",

                            table));

            log.debug("WRITE DETECTED >>> {} -> {}", objectName, table);
        }

        return relations.stream().distinct().toList();
    }

    private List<String> extractReadTables(String sourceCode) {

        Set<String> tables = new HashSet<>();

        String normalized = sourceCode.toUpperCase();

        Pattern fromPattern = Pattern.compile("\\bFROM\\b", Pattern.CASE_INSENSITIVE);

        Matcher matcher = fromPattern.matcher(normalized);

        while (matcher.find()) {

            int fromIndex = matcher.start();

            String remaining = normalized.substring(fromIndex);

            String fromClause = extractTopLevelFromClause(remaining);

        log.debug("FROM CLAUSE >>> {}", fromClause);

            List<String> parts = splitTopLevelComma(fromClause);

            for (String part : parts) {

                log.debug("PART >>> {}", part);

                String trimmed = part.trim();

                // =============================
                // SUBQUERY
                // =============================

                if (trimmed.startsWith("(")) {

                    tables.addAll(extractReadTables(trimmed));

                    continue;
                }

                // =============================
                // NORMAL TABLE
                // =============================

                String table = trimmed.split("\\s+")[0];

                table = clean(table);

                if (isValidTable(table)) {

                    tables.add(table);
                    log.debug("READ TABLE >>> {}", table);
                }
            }
        }
        Pattern joinPattern = Pattern.compile("\\bJOIN\\s+([A-Z][A-Z0-9_]*(?:\\.[A-Z][A-Z0-9_]*)?)",
                Pattern.CASE_INSENSITIVE);

        Matcher joinMatcher = joinPattern.matcher(normalized);

        while (joinMatcher.find()) {

            String table = clean(joinMatcher.group(1));

            if (isValidTable(table)) {

                tables.add(table);

                log.debug("JOIN TABLE >>> {}", table);
            }
        }



        return tables.stream().distinct().toList();
    }

    private List<String> extractWriteTables(String sourceCode) {

        List<String> result = new ArrayList<>();

        // UPDATE

        Pattern updatePattern = Pattern.compile(
                "\\bUPDATE\\s+([A-Z][A-Z0-9_]*(?:\\.[A-Z][A-Z0-9_]*)?)", Pattern.CASE_INSENSITIVE);

        Matcher updateMatcher = updatePattern.matcher(sourceCode);

        while (updateMatcher.find()) {

            String table = updateMatcher.group(1).toUpperCase();

            if (isValidTable(table)) {

                result.add(table);
            }
        }

        // INSERT INTO

        Pattern insertPattern =
                Pattern.compile("\\bINSERT\\s+INTO\\s+([A-Z][A-Z0-9_]*(?:\\.[A-Z][A-Z0-9_]*)?)",
                        Pattern.CASE_INSENSITIVE);

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
