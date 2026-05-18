package com.ignacio.legacyanalyzer.infrastructure.adapters.output.parser;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ViewDependencyExtractor {

  private static final Pattern VIEW_PATTERN =
      Pattern.compile("(?is)CREATE\\s+(OR\\s+REPLACE\\s+)?VIEW\\s+([A-Z0-9_.$#]+)\\s+AS\\s+(.*?);");

  private static final Pattern FROM_JOIN_PATTERN =
      Pattern.compile("(?i)\\b(FROM|JOIN)\\s+([A-Z0-9_.$#]+)");

  public Set<String> extractRelations(String sourceCode) {

    Set<String> relations = new LinkedHashSet<>();

    Matcher viewMatcher = VIEW_PATTERN.matcher(sourceCode);

    while (viewMatcher.find()) {

      String viewName = clean(viewMatcher.group(2));

      String query = viewMatcher.group(3);

      Matcher dependencyMatcher = FROM_JOIN_PATTERN.matcher(query);

      while (dependencyMatcher.find()) {

        String dependency = clean(dependencyMatcher.group(2));

        if (!isValidObject(dependency)) {
          continue;
        }

        relations.add(viewName + "->" + dependency);
      }
    }

    return relations;
  }

  private String clean(String value) {

    return value == null ? "" : value.replaceAll("[,;()]", "").trim().toUpperCase();
  }

  private boolean isValidObject(String value) {

    return value != null && !value.isBlank() && value.length() > 1
        && value.matches("[A-Z][A-Z0-9_.$#]*") && !Set.of("SELECT", "FROM", "WHERE", "JOIN", "LEFT",
            "RIGHT", "INNER", "OUTER", "ON", "GROUP", "ORDER", "BY").contains(value);
  }
}
