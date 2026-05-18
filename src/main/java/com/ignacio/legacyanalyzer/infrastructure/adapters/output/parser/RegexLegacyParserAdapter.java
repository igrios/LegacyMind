package com.ignacio.legacyanalyzer.infrastructure.adapters.output.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import com.ignacio.legacyanalyzer.domain.model.JoinCondition;
import com.ignacio.legacyanalyzer.domain.model.KnowledgeRelation;
import com.ignacio.legacyanalyzer.domain.model.LegacyObject;
import com.ignacio.legacyanalyzer.domain.model.RiskFinding;
import com.ignacio.legacyanalyzer.domain.model.SqlSemanticModel;
import com.ignacio.legacyanalyzer.domain.model.SubprogramNode;
import com.ignacio.legacyanalyzer.domain.model.TableReference;
import com.ignacio.legacyanalyzer.domain.ports.LegacyParserPort;
import com.ignacio.legacyanalyzer.domain.services.GraphRelationExtractor;
import com.ignacio.legacyanalyzer.domain.services.LegacyRiskAnalyzer;
import com.ignacio.legacyanalyzer.domain.services.SqlSemanticExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RegexLegacyParserAdapter implements LegacyParserPort {

    private final LegacyRiskAnalyzer riskAnalyzer;
    private final SqlSemanticExtractor semanticExtractor;
    private final GraphRelationExtractor graphRelationExtractor;


    private static final Pattern NAME_PATTERN = Pattern.compile(
            "CREATE\\s+OR\\s+REPLACE\\s+(PACKAGE|PROCEDURE|FUNCTION)\\s+(BODY\\s+)?(\\w+)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern PROCEDURE_PATTERN =
            Pattern.compile("\\bPROCEDURE\\s+(\\w+)\\s*(\\(|IS|AS)", Pattern.CASE_INSENSITIVE);

    private static final Pattern DELETE_PATTERN =
            Pattern.compile("\\bDELETE\\s+FROM\\s+([A-Z][A-Z0-9_]*(?:\\.[A-Z][A-Z0-9_]*)?)",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern UPDATE_PATTERN = Pattern.compile(
            "\\bUPDATE\\s+([A-Z][A-Z0-9_]*(?:\\.[A-Z][A-Z0-9_]*)?)", Pattern.CASE_INSENSITIVE);

    private static final Pattern INSERT_PATTERN =
            Pattern.compile("\\bINSERT\\s+INTO\\s+([A-Z][A-Z0-9_]*(?:\\.[A-Z][A-Z0-9_]*)?)",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern SIMPLE_UPDATE_PATTERN =
            Pattern.compile("\\bUPDATE\\s+(\\w+)", Pattern.CASE_INSENSITIVE);


    private static final Pattern JOIN_PATTERN =
            Pattern.compile("\\bJOIN\\s+([A-Z][A-Z0-9_.$#@]*(?:\\.[A-Z][A-Z0-9_.$#@]*)?)",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern JOIN_ALIAS_PATTERN = Pattern.compile(
            "\\bJOIN\\s+([A-Z][A-Z0-9_.$#@]*(?:\\.[A-Z][A-Z0-9_.$#@]*)?)\\s+([A-Z][A-Z0-9_]*)",
            Pattern.CASE_INSENSITIVE);



    @Override
    public LegacyObject parse(String sourceCode) {

        String cleanSource = preProcess(sourceCode);

        String normalized = normalizeSql(cleanSource);

        String sql = normalized.replaceAll("\\b[A-Z_]+\\s*\\([^()]*\\)", " ");

        // =============================
        // NAME + TYPE
        // =============================

        String name = null;

        String type = null;

        Matcher nameMatcher = NAME_PATTERN.matcher(sourceCode);

        if (nameMatcher.find()) {

            type = nameMatcher.group(1).toUpperCase();

            name = nameMatcher.group(3).toUpperCase();
        }

        // =============================
        // PROCEDURES
        // =============================

        List<String> procedures = new ArrayList<>();

        Matcher procMatcher = PROCEDURE_PATTERN.matcher(sourceCode);

        while (procMatcher.find()) {

            String procedure = procMatcher.group(1).toUpperCase();

            if (name == null || !procedure.equals(name)) {

                procedures.add(procedure);
            }
        }

        procedures = procedures.stream().distinct().toList();

        // =============================
        // SUBPROGRAMS
        // =============================

        SubprogramExtractor subprogramExtractor = new SubprogramExtractor();

        List<SubprogramNode> subprograms = subprogramExtractor.extract(sourceCode, name);

        log.debug("SUBPROGRAMS >>> {}", subprograms.size());

        log.debug("FROM CLAUSE >>> {}", extractTopLevelFromClause(sql));

        // =============================
        // READ TABLES
        // =============================

        List<String> readTables = semanticExtractor.extractReadTables(normalized);

        log.debug("READ TABLES FINAL >>> {}", readTables);

        // =============================
        // WRITE TABLES
        // =============================

        List<String> writeTables = semanticExtractor.extractWriteTables(normalized);

        log.debug("WRITE TABLES FINAL >>> {}", writeTables);

        // =============================
        // SEMANTIC MODEL
        // =============================

        SqlSemanticModel semanticModel = buildSemanticModel(normalized);

        riskAnalyzer.analyzeRisks(semanticModel);

        // =============================
        // KNOWLEDGE RELATIONS
        // =============================
List<KnowledgeRelation> knowledgeRelations =
        graphRelationExtractor
                .extractKnowledgeRelations(
                        sourceCode,
                        name);
        // =============================
        // REFERENCED TABLES
        // =============================

        List<String> referencedTables =
                Stream.concat(readTables.stream(), writeTables.stream()).distinct().toList();

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

                semanticModel.getFindings().stream().map(RiskFinding::toString).toList(),

                semanticModel.getRiskScore(),

                semanticModel.getRiskLevel(),

                buildSummary(type, name, referencedTables, semanticModel));
    }

    private String buildSummary(String type, String name, List<String> referencedTables,
            SqlSemanticModel model) {

        return String.format("The %s %s interacts with %d tables and has a risk level of %s.", type,
                name, referencedTables.size(), model.getRiskLevel());
    }


    public List<String> extractSemanticRelations(String sql) {

        log.debug("NEW SEMANTIC RELATIONS RUNNING");

        Set<String> relations = new LinkedHashSet<>();

        sql = normalizeSql(sql);

        String mainTable = detectMainTable(sql);

        String[] statements = sql.split(";");

        for (String stmt : statements) {

            log.debug("STATEMENT >>> {}", stmt);

            // =============================
            // UPDATE RELATIONS
            // =============================

            Matcher updateMatcher = UPDATE_PATTERN.matcher(stmt);

            if (updateMatcher.find()) {

                String target = clean(updateMatcher.group(1));

                if (isValidTable(target) && mainTable != null && !target.equals(mainTable)) {

                    relations.add(mainTable + "->" + target);
                }
            }

            // =============================
            // INSERT RELATIONS
            // =============================

            Matcher insertMatcher = INSERT_PATTERN.matcher(stmt);

            if (insertMatcher.find()) {

                String target = clean(insertMatcher.group(1));

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
            // SELECT / JOIN RELATIONS
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

        Matcher updateMatcher = SIMPLE_UPDATE_PATTERN.matcher(normalized);


        if (updateMatcher.find()) {

            String table = clean(updateMatcher.group(1));

            if (isValidTable(table)) {
                return table;
            }
        }

        // PRIORIDAD 2:
        // inferir por frecuencia de lectura

        Map<String, Integer> frequency = new HashMap<>();

        for (String table : semanticExtractor.extractReadTables(normalized)) {

            frequency.put(table, frequency.getOrDefault(table, 0) + 1);
        }

        return frequency.entrySet().stream().max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse(null);
    }

    // =============================
    // HELPERS
    // =============================

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

    private String normalizeSql(String sql) {

        return sql.replace("\r", " ").replace("\n", " ").replaceAll("\\s+", " ").trim()
                .toUpperCase();
    }



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

                List<String> nestedTables = semanticExtractor.extractReadTables(trimmed);

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


            if (table != null && !table.isBlank()) {

                tables.add(table);

                log.debug("EXTRACT TABLE >>> {}", table);
            }
        }

        Matcher joinMatcher = JOIN_PATTERN.matcher(clause);

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

        String normalized = normalizeSql(sql);


        model.setOriginalSql(sql);

        List<String> readTables = semanticExtractor.extractReadTables(normalized);

        List<String> writeTables = semanticExtractor.extractWriteTables(normalized);

        List<String> semanticRelations = extractSemanticRelations(normalized);

        String fromClause = extractTopLevelFromClause(normalized);

        List<TableReference> references = extractTableReferences(fromClause);

        List<JoinCondition> joins = extractJoinConditions(normalized);

        model.setReadTables(readTables);

        model.setWriteTables(writeTables);

        model.setSemanticRelations(semanticRelations);

        model.setTableReferences(references);

        model.setJoinConditions(joins);

        riskAnalyzer.analyzeRisks(model);

        return model;
    }



    public boolean hasDeleteWithoutWhere(String sql) {

        Matcher deleteMatcher = DELETE_PATTERN.matcher(sql);

        while (deleteMatcher.find()) {

            int deleteStart = deleteMatcher.start();

            int nextSemicolon = sql.indexOf(";", deleteStart);

            String statement;

            if (nextSemicolon == -1) {

                statement = sql.substring(deleteStart);

            } else {

                statement = sql.substring(deleteStart, nextSemicolon);
            }

            if (!statement.contains(" WHERE ")) {

                log.debug("DELETE WITHOUT WHERE >>> {}", statement);

                return true;
            }
        }

        return false;
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




    private boolean isValidTable(String table) {

        return table != null && !table.isBlank() && table.length() > 1
                && table.matches("[A-Z][A-Z0-9_.$#@]{1,}")
                && !Set.of("SELECT", "FROM", "WHERE", "INSERT", "INTO", "VALUES", "UPDATE", "SET",
                        "DELETE", "JOIN", "LEFT", "RIGHT", "INNER", "OUTER", "ON", "AND", "OR",
                        "GROUP", "ORDER", "BY", "BEGIN", "END", "NULL").contains(table);
    }

    private String clean(String table) {

        return table.replaceAll("[^A-Z0-9_.$#@]", "");
    }

}
