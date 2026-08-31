package com.ignacio.legacyanalyzer.domain.services.semantic;

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

  private static final Set<String> ORACLE_RESERVED_IDENTIFIERS = Set.of(
      "SELECT", "FROM", "WHERE", "INSERT", "INTO", "VALUES", "UPDATE", "SET", "DELETE",
      "JOIN", "LEFT", "RIGHT", "INNER", "OUTER", "ON", "AND", "OR", "GROUP", "ORDER",
      "BY", "BEGIN", "END", "NULL", "IS", "AS", "LOOP", "FOR", "WHEN", "OTHERS", "DE",
      "OF", "DUAL", "SYSDATE", "USER", "ROWNUM", "NEXTVAL", "CURRVAL", "SQLERRM",
      "SQLCODE");

  private static final String ORACLE_IDENTIFIER = "[A-Z][A-Z0-9_$#]*";
  private static final Pattern TABLE_TARGET_PATTERN = Pattern.compile(
      ORACLE_IDENTIFIER + "(?:\\." + ORACLE_IDENTIFIER + ")?(?:@" + ORACLE_IDENTIFIER + ")?");
  private static final Pattern CALL_TARGET_PATTERN = Pattern.compile(
      ORACLE_IDENTIFIER + "(?:\\." + ORACLE_IDENTIFIER + ")?");
  private static final String TABLE_TARGET_CAPTURE =
      "(" + ORACLE_IDENTIFIER + "(?:\\." + ORACLE_IDENTIFIER + ")?(?:@"
          + ORACLE_IDENTIFIER + ")?)";

  private static final Pattern FROM_PATTERN =
      Pattern.compile("\\bFROM\\b", Pattern.CASE_INSENSITIVE);

  private static final Pattern JOIN_ALIAS_PATTERN = Pattern.compile(
      "\\bJOIN\\s+" + TABLE_TARGET_CAPTURE,
      Pattern.CASE_INSENSITIVE);

  private static final Pattern TABLE_SOURCE_PATTERN = Pattern.compile(
      TABLE_TARGET_CAPTURE,
      Pattern.CASE_INSENSITIVE);

  private static final Pattern UPDATE_PATTERN = Pattern
      .compile("\\bUPDATE\\s+" + TABLE_TARGET_CAPTURE, Pattern.CASE_INSENSITIVE);

  private static final Pattern INSERT_PATTERN = Pattern.compile(
      "\\bINSERT\\s+INTO\\s+" + TABLE_TARGET_CAPTURE, Pattern.CASE_INSENSITIVE);

  private static final Pattern MERGE_PATTERN = Pattern.compile(
      "\\bMERGE\\s+INTO\\s+" + TABLE_TARGET_CAPTURE,
      Pattern.CASE_INSENSITIVE);

  private static final Pattern DELETE_PATTERN = Pattern.compile(
      "\\bDELETE\\s+FROM\\s+" + TABLE_TARGET_CAPTURE,
      Pattern.CASE_INSENSITIVE);


  public List<String> extractReadTables(String sourceCode) {

    Set<String> tables = new HashSet<>();

    String normalized = maskNonCode(sourceCode).toUpperCase();

    Matcher matcher = FROM_PATTERN.matcher(normalized);

    while (matcher.find()) {

      if (isDeleteFrom(normalized, matcher.start())) {
        continue;
      }

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
          continue;
        }

        // =============================
        // NORMAL TABLE
        // =============================

        Matcher sourceMatcher = TABLE_SOURCE_PATTERN.matcher(trimmed);
        if (!sourceMatcher.lookingAt()) {
          continue;
        }

        String table = sourceMatcher.group(1).toUpperCase();

        if (isValidTableTarget(table)) {

          tables.add(table);

          log.debug("READ TABLE >>> {}", table);
        }
      }
    }

    Matcher joinMatcher = JOIN_ALIAS_PATTERN.matcher(normalized);

    while (joinMatcher.find()) {

      String table = clean(joinMatcher.group(1));

      if (isValidTableTarget(table)) {

        tables.add(table);

        log.debug("JOIN TABLE >>> {}", table);
      }
    }

    return tables.stream().distinct().toList();
  }

  public List<String> extractWriteTables(String sourceCode) {

    List<String> result = new ArrayList<>();
    String normalized = maskNonCode(sourceCode).toUpperCase();

    // =============================
    // UPDATE
    // =============================

    Matcher updateMatcher = UPDATE_PATTERN.matcher(normalized);

    while (updateMatcher.find()) {

      if (isCursorForUpdate(normalized, updateMatcher.start())) {
        continue;
      }

      String table = updateMatcher.group(1).toUpperCase();

      if (isValidTableTarget(table)) {

        result.add(table);

        log.debug("UPDATE TABLE >>> {}", table);
      }
    }

    // =============================
    // INSERT INTO
    // =============================

    Matcher insertMatcher = INSERT_PATTERN.matcher(normalized);

    while (insertMatcher.find()) {

      String table = insertMatcher.group(1).toUpperCase();

      if (isValidTableTarget(table)) {

        result.add(table);

        log.debug("INSERT TABLE >>> {}", table);
      }
    }

Matcher mergeMatcher = MERGE_PATTERN.matcher(normalized);

while (mergeMatcher.find()) {

    String table = mergeMatcher.group(1).toUpperCase();

    if (isValidTableTarget(table)) {

        result.add(table);

        log.debug("MERGE TARGET TABLE >>> {}", table);
    }
}

    Matcher deleteMatcher = DELETE_PATTERN.matcher(normalized);

    while (deleteMatcher.find()) {
      String table = deleteMatcher.group(1).toUpperCase();
      if (isValidTableTarget(table)) {
        result.add(table);
        log.debug("DELETE TABLE >>> {}", table);
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

  /**
   * Validates an Oracle table identifier already captured from a table-bearing SQL clause.
   * Callers must not use this method to infer table semantics from an arbitrary token.
   */
  public boolean isValidTableTarget(String target) {

    if (target == null || target.isBlank()) {

      return false;
    }

    String table = target.trim().toUpperCase();

    if (!TABLE_TARGET_PATTERN.matcher(table).matches()) {
      return false;
    }

    String withoutDbLink = table.contains("@")
        ? table.substring(0, table.indexOf('@'))
        : table;
    String localName = withoutDbLink.contains(".")
        ? withoutDbLink.substring(withoutDbLink.lastIndexOf('.') + 1)
        : withoutDbLink;
    String qualifier = withoutDbLink.contains(".")
        ? withoutDbLink.substring(0, withoutDbLink.indexOf('.'))
        : "";

    // =============================================
    // BASIC HARDENING
    // =============================================

    return !ORACLE_RESERVED_IDENTIFIERS.contains(localName)
        && !ORACLE_RESERVED_IDENTIFIERS.contains(qualifier);
  }

  /** Validates package and subprogram graph targets independently from PL/SQL variable naming. */
  public boolean isValidCallTarget(String target) {
    if (target == null || target.isBlank()) {
      return false;
    }
    String normalized = target.trim().toUpperCase();
    if (!CALL_TARGET_PATTERN.matcher(normalized).matches()) {
      return false;
    }
    String[] parts = normalized.split("\\.");
    for (String part : parts) {
      if (ORACLE_RESERVED_IDENTIFIERS.contains(part)) {
        return false;
      }
    }
    return true;
  }

  private boolean isCursorForUpdate(String sql, int updateStart) {
    String prefix = sql.substring(Math.max(0, updateStart - 20), updateStart);
    return prefix.matches("(?s).*\\bFOR\\s+$");
  }

  private boolean isDeleteFrom(String sql, int fromStart) {
    String prefix = sql.substring(Math.max(0, fromStart - 20), fromStart);
    return prefix.matches("(?s).*\\bDELETE\\s+$");
  }

  private String maskNonCode(String sourceCode) {
    StringBuilder masked = new StringBuilder(sourceCode);
    maskMatches(masked, Pattern.compile("--[^\\r\\n]*").matcher(sourceCode));
    maskMatches(masked, Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL).matcher(masked));
    maskMatches(masked, Pattern.compile("'(?:''|[^'])*'").matcher(masked));
    return masked.toString();
  }

  private void maskMatches(StringBuilder value, Matcher matcher) {
    while (matcher.find()) {
      for (int i = matcher.start(); i < matcher.end(); i++) {
        char current = value.charAt(i);
        if (current != '\n' && current != '\r') {
          value.setCharAt(i, ' ');
        }
      }
    }
  }
}
