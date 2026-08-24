package com.ignacio.legacyanalyzer.infrastructure.adapters.output.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import com.ignacio.legacyanalyzer.domain.model.TableReference;
import com.ignacio.legacyanalyzer.domain.services.semantic.SqlSemanticExtractor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SemanticDependencyExtractor {

    private static final Pattern UPDATE_PATTERN = Pattern.compile(
            "\\bUPDATE\\s+([A-Z][A-Z0-9_]*(?:\\.[A-Z][A-Z0-9_]*)?)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern INSERT_PATTERN = Pattern.compile(
            "\\bINSERT\\s+INTO\\s+([A-Z][A-Z0-9_]*(?:\\.[A-Z][A-Z0-9_]*)?)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SIMPLE_UPDATE_PATTERN = Pattern.compile(
            "\\bUPDATE\\s+(\\w+)", Pattern.CASE_INSENSITIVE);

    private final SqlSemanticExtractor semanticExtractor;
    private final OracleJoinExtractor joinExtractor;

    public SemanticDependencyExtractor(
            SqlSemanticExtractor semanticExtractor, OracleJoinExtractor joinExtractor) {
        this.semanticExtractor = semanticExtractor;
        this.joinExtractor = joinExtractor;
    }

    public List<String> extract(String sql) {
        Set<String> relations = new LinkedHashSet<>();
        String normalized = normalize(sql);
        String mainTable = detectMainTable(normalized);

        for (String statement : normalized.split(";")) {
            Matcher updateMatcher = UPDATE_PATTERN.matcher(statement);
            if (updateMatcher.find()) {
                String target = joinExtractor.clean(updateMatcher.group(1));
                if (semanticExtractor.isValidTable(target) && mainTable != null
                        && !target.equals(mainTable)) {
                    relations.add(mainTable + "->" + target);
                }
            }

            Matcher insertMatcher = INSERT_PATTERN.matcher(statement);
            if (insertMatcher.find()) {
                String target = joinExtractor.clean(insertMatcher.group(1));
                if (semanticExtractor.isValidTable(target)) {
                    for (String source : extractTables(
                            joinExtractor.extractTopLevelFromClause(statement))) {
                        if (!source.equals(target)) relations.add(source + "->" + target);
                    }
                }
            }

            String fromClause = joinExtractor.extractTopLevelFromClause(statement);
            if (fromClause != null && !fromClause.isBlank()) {
                List<String> tables = joinExtractor.extractTableReferences(fromClause).stream()
                        .map(TableReference::getFullName)
                        .map(joinExtractor::clean)
                        .filter(semanticExtractor::isValidTable)
                        .distinct().toList();
                for (int i = 0; i < tables.size(); i++) {
                    for (int j = i + 1; j < tables.size(); j++) {
                        if (!tables.get(i).equals(tables.get(j))) {
                            relations.add(tables.get(i) + "->" + tables.get(j));
                        }
                    }
                }
            }
        }
        log.debug("RELATIONS >>> {}", relations);
        return new ArrayList<>(relations);
    }

    private String detectMainTable(String normalized) {
        Matcher updateMatcher = SIMPLE_UPDATE_PATTERN.matcher(normalized);
        if (updateMatcher.find()) {
            String table = joinExtractor.clean(updateMatcher.group(1));
            if (semanticExtractor.isValidTable(table)) return table;
        }
        Map<String, Integer> frequency = new HashMap<>();
        for (String table : semanticExtractor.extractReadTables(normalized)) {
            frequency.put(table, frequency.getOrDefault(table, 0) + 1);
        }
        return frequency.entrySet().stream().max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse(null);
    }

    private Set<String> extractTables(String clause) {
        Set<String> tables = new HashSet<>();
        if (clause == null || clause.isBlank()) return tables;
        for (TableReference reference : joinExtractor.extractTableReferences(clause)) {
            if (reference.getFullName() != null) {
                String table = joinExtractor.clean(reference.getFullName().toUpperCase());
                if (semanticExtractor.isValidTable(table)) tables.add(table);
            }
        }
        for (String part : joinExtractor.splitTopLevelComma(clause)) {
            if (part.trim().startsWith("(")) {
                tables.addAll(semanticExtractor.extractReadTables(part.trim()));
            }
        }
        return tables;
    }

    public String normalize(String sql) {
        return sql.replace("\r", " ").replace("\n", " ").replaceAll("\\s+", " ")
                .trim().toUpperCase();
    }

    public String normalizeSourceCode(String sourceCode) {
        String withoutCommentsAndLiterals = sourceCode.replaceAll("/\\*.*?\\*/", " ")
                .replaceAll("--.*?(\\r?\\n)", " ")
                .replaceAll("'(?:''|[^'])*'", "''");
        return normalize(withoutCommentsAndLiterals);
    }
}
