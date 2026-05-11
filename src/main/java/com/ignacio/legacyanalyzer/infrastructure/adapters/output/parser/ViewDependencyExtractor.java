package com.ignacio.legacyanalyzer.infrastructure.adapters.output.parser;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ViewDependencyExtractor {

  private static final Pattern VIEW_PATTERN =
      Pattern.compile("(?is)CREATE\\s+(OR\\s+REPLACE\\s+)?VIEW\\s+([A-Z0-9_]+)\\s+AS\\s+(.*?);");

  private static final Pattern FROM_PATTERN = Pattern.compile("(?i)(FROM|JOIN)\\s+([A-Z0-9_]+)");

  public Set<String> extractRelations(String sourceCode) {

    Set<String> relations = new HashSet<>();

    Matcher viewMatcher = VIEW_PATTERN.matcher(sourceCode);

    while (viewMatcher.find()) {

      String viewName = viewMatcher.group(2);

      String query = viewMatcher.group(3);

      Matcher fromMatcher = FROM_PATTERN.matcher(query);

      while (fromMatcher.find()) {

        String dependency = fromMatcher.group(2);

        relations.add(viewName + "->" + dependency);
      }
    }

    return relations;
  }
}
