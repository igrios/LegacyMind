package com.ignacio.legacyanalyzer.domain.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import com.ignacio.legacyanalyzer.domain.model.TableReference;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SqlSemanticExtractor {

  private static final Pattern FROM_PATTERN =
      Pattern.compile("\\bFROM\\b", Pattern.CASE_INSENSITIVE);

  private static final Pattern JOIN_ALIAS_PATTERN = Pattern.compile(
      "\\bJOIN\\s+([A-Z][A-Z0-9_.$#@]*(?:\\.[A-Z][A-Z0-9_.$#@]*)?)\\s+([A-Z][A-Z0-9_]*)",
      Pattern.CASE_INSENSITIVE);

  private static final Pattern UPDATE_PATTERN = Pattern
      .compile("\\bUPDATE\\s+([A-Z][A-Z0-9_]*(?:\\.[A-Z][A-Z0-9_]*)?)", Pattern.CASE_INSENSITIVE);

  private static final Pattern INSERT_PATTERN = Pattern.compile(
      "\\bINSERT\\s+INTO\\s+([A-Z][A-Z0-9_]*(?:\\.[A-Z][A-Z0-9_]*)?)", Pattern.CASE_INSENSITIVE);

private static final Pattern MERGE_PATTERN =
    Pattern.compile(
        "\\bMERGE\\s+INTO\\s+([A-Z][A-Z0-9_]*(?:\\.[A-Z][A-Z0-9_]*)?)",
        Pattern.CASE_INSENSITIVE);

private static final Pattern USING_PATTERN =
    Pattern.compile(
        "\\bUSING\\s+([A-Z][A-Z0-9_]*(?:\\.[A-Z][A-Z0-9_]*)?)",
        Pattern.CASE_INSENSITIVE);


  public List<String> extractReadTables(String sourceCode) {

    Set<String> tables = new HashSet<>();

    String normalized = sourceCode.toUpperCase();

    Matcher matcher = FROM_PATTERN.matcher(normalized);

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

    Matcher joinMatcher = JOIN_ALIAS_PATTERN.matcher(normalized);

    while (joinMatcher.find()) {

      String table = clean(joinMatcher.group(1));

      if (isValidTable(table)) {

        tables.add(table);

        log.debug("JOIN TABLE >>> {}", table);
      }
    }

    Matcher usingMatcher = USING_PATTERN.matcher(normalized);

while (usingMatcher.find()) {

    String table = clean(usingMatcher.group(1));

    if (isValidTable(table)) {

        tables.add(table);

        log.debug("MERGE USING TABLE >>> {}", table);
    }
}

    return tables.stream().distinct().toList();
  }

  public List<String> extractWriteTables(String sourceCode) {

    List<String> result = new ArrayList<>();

    // =============================
    // UPDATE
    // =============================

    Matcher updateMatcher = UPDATE_PATTERN.matcher(sourceCode);

    while (updateMatcher.find()) {

      String table = updateMatcher.group(1).toUpperCase();

      if (isValidTable(table)) {

        result.add(table);

        log.debug("UPDATE TABLE >>> {}", table);
      }
    }

    // =============================
    // INSERT INTO
    // =============================

    Matcher insertMatcher = INSERT_PATTERN.matcher(sourceCode);

    while (insertMatcher.find()) {

      String table = insertMatcher.group(1).toUpperCase();

      if (isValidTable(table)) {

        result.add(table);

        log.debug("INSERT TABLE >>> {}", table);
      }
    }

Matcher mergeMatcher = MERGE_PATTERN.matcher(sourceCode);

while (mergeMatcher.find()) {

    String table = mergeMatcher.group(1).toUpperCase();

    if (isValidTable(table)) {

        result.add(table);

        log.debug("MERGE TARGET TABLE >>> {}", table);
    }
}

    return result.stream().distinct().toList();
  }

  public String extractTopLevelFromClause(String sql) {

    String normalized = sql.replaceAll("\\s+", " ").trim();

    Matcher matcher = FROM_PATTERN.matcher(normalized);

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

        if (remaining.startsWith("WHERE ") || remaining.startsWith("GROUP BY ")
            || remaining.startsWith("ORDER BY ") || remaining.startsWith("CONNECT BY ")
            || remaining.startsWith("HAVING ") || remaining.startsWith("UNION ")
            || remaining.startsWith("INTERSECT ") || remaining.startsWith("MINUS ")
            || remaining.startsWith(";")) {

          break;
        }
      }

      fromClause.append(current);
    }

    return fromClause.toString().trim();
  }

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

  public Map<String, String> buildAliasMap(List<TableReference> references) {

    Map<String, String> aliases = new HashMap<>();

    for (TableReference ref : references) {

      if (ref.getAlias() != null) {

        aliases.put(ref.getAlias().toUpperCase(), ref.getFullName());
      }
    }

    return aliases;
  }

  private String clean(String table) {

    return table.replaceAll("[^A-Z0-9_.$#@]", "");
  }

  public boolean isValidTable(String table) {

    if (table == null || table.isBlank()) {

      return false;
    }

    table = table.trim().toUpperCase();

    // =============================================
    // BASIC HARDENING
    // =============================================

   if (table.length() == 1) {

    return table.matches("[A-Z]");
}

    // =============================================
    // SQL BLACKLIST
    // =============================================

    if (Set.of(

        "SELECT", "FROM", "WHERE", "INSERT", "INTO", "VALUES", "UPDATE", "SET", "DELETE", "JOIN",
        "LEFT", "RIGHT", "INNER", "OUTER", "ON", "AND", "OR", "GROUP", "ORDER", "BY", "BEGIN",
        "END", "NULL", "IS", "AS", "LOOP", "FOR", "WHEN", "OTHERS", "DE"

    ).contains(table)) {

      return false;
    }

    // =============================================
    // CLIENTES
    // STOCK_DEPOSITO
    // =============================================

    if (table.matches("[A-Z][A-Z0-9_#$@]*")) {

      return true;
    }

    // =============================================
    // CRM.CLIENTES
    // ERP.PEDIDOS
    // =============================================

    if (table.matches("[A-Z][A-Z0-9_#$@]*\\.[A-Z][A-Z0-9_#$@]*")) {

      return true;
    }

    return false;
  }
}
