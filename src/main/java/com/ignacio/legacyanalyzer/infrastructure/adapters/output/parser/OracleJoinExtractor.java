package com.ignacio.legacyanalyzer.infrastructure.adapters.output.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import com.ignacio.legacyanalyzer.domain.model.JoinCondition;
import com.ignacio.legacyanalyzer.domain.model.TableReference;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class OracleJoinExtractor {

    public String extractTopLevelFromClause(String sql) {
        String normalized = sql.replaceAll("\\s+", " ").trim();
        Matcher matcher = Pattern.compile("\\bFROM\\b", Pattern.CASE_INSENSITIVE).matcher(normalized);
        if (!matcher.find()) {
            return "";
        }
        int depth = 0;
        StringBuilder clause = new StringBuilder();
        for (int i = matcher.end(); i < normalized.length(); i++) {
            char current = normalized.charAt(i);
            if (current == '(') depth++;
            if (current == ')') depth--;
            if (depth == 0) {
                String remaining = normalized.substring(i).toUpperCase();
                if (remaining.startsWith("WHERE ") || remaining.startsWith("GROUP BY ")
                        || remaining.startsWith("ORDER BY ") || remaining.startsWith("CONNECT BY ")
                        || remaining.startsWith("HAVING ") || remaining.startsWith("UNION ")
                        || remaining.startsWith("INTERSECT ") || remaining.startsWith("MINUS ")
                        || remaining.startsWith(";")) {
                    break;
                }
            }
            clause.append(current);
        }
        return clause.toString().trim();
    }

    public List<TableReference> extractTableReferences(String clause) {
        List<TableReference> references = new ArrayList<>();
        if (clause == null || clause.isBlank()) return references;

        for (String part : splitTopLevelComma(clause)) {
            String trimmed = part.trim();
            if (trimmed.startsWith("(")) continue;
            String[] tokens = trimmed.split("\\s+");
            if (tokens.length == 0) continue;
            String table = clean(tokens[0]);
            String alias = null;
            if (tokens.length > 1) {
                String candidate = tokens[1].toUpperCase();
                if (!Set.of("INNER", "LEFT", "RIGHT", "JOIN", "ON").contains(candidate)) {
                    alias = candidate;
                }
            }
            references.add(new TableReference(table, alias));
            log.debug("TABLE REF >>> {} alias={}", table, alias);
        }

        Matcher joinMatcher = Pattern.compile(
                "\\bJOIN\\s+([A-Z][A-Z0-9_.$#@]*(?:\\.[A-Z][A-Z0-9_.$#@]*)?)\\s+([A-Z][A-Z0-9_]*)",
                Pattern.CASE_INSENSITIVE).matcher(clause);
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
        String withoutOuterJoinMarker = sql.replaceAll("\\(\\+\\)", "");
        Matcher matcher = Pattern.compile(
                "([A-Z][A-Z0-9_]*)\\.([A-Z][A-Z0-9_]*)\\s*=\\s*([A-Z][A-Z0-9_]*)\\.([A-Z][A-Z0-9_]*)",
                Pattern.CASE_INSENSITIVE).matcher(withoutOuterJoinMarker.toUpperCase());
        while (matcher.find()) {
            String leftAlias = matcher.group(1);
            String rightAlias = matcher.group(3);
            if (!isValidSqlAlias(leftAlias) || !isValidSqlAlias(rightAlias)) {
                log.warn("IGNORING INVALID JOIN ALIAS >>> {} -> {}", leftAlias, rightAlias);
                continue;
            }
            JoinCondition condition = new JoinCondition(
                    leftAlias, matcher.group(2), rightAlias, matcher.group(4));
            conditions.add(condition);
            log.debug("JOIN CONDITION >>> {}", condition);
        }
        return conditions;
    }

    public void resolveJoinConditions(List<TableReference> references, List<JoinCondition> conditions) {
        Map<String, String> aliasMap = new HashMap<>();
        for (TableReference reference : references) {
            if (reference.getAlias() != null) {
                aliasMap.put(reference.getAlias().toUpperCase(), reference.getFullName());
            }
        }
        log.debug("ALIAS MAP >>> {}", aliasMap);
        for (JoinCondition condition : conditions) {
            String leftTable = aliasMap.get(condition.getLeftAlias().toUpperCase());
            String rightTable = aliasMap.get(condition.getRightAlias().toUpperCase());
            if (leftTable == null || rightTable == null) {
                log.warn("INVALID JOIN ALIAS DETECTED >>> {} -> {}",
                        condition.getLeftAlias(), condition.getRightAlias());
                continue;
            }
            log.debug("RESOLVED JOIN >>> {}.{} -> {}.{}", leftTable, condition.getLeftColumn(),
                    rightTable, condition.getRightColumn());
        }
    }

    public List<String> splitTopLevelComma(String text) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '(') depth++;
            if (c == ')') depth--;
            if (c == ',' && depth == 0) {
                result.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (!current.isEmpty()) result.add(current.toString().trim());
        return result;
    }

    private boolean isValidSqlAlias(String alias) {
        if (alias == null || alias.isBlank()) return false;
        String normalized = alias.toUpperCase();
        return !Set.of("SELECT", "FROM", "WHERE", "JOIN", "LEFT", "RIGHT", "INNER", "OUTER",
                "ON", "AND", "OR").contains(normalized) && !normalized.matches("X{2,}");
    }

    public String clean(String table) {
        return table.replaceAll("[^A-Z0-9_.$#@]", "");
    }
}
