package com.ignacio.legacyanalyzer.domain.services;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.ignacio.legacyanalyzer.domain.model.ExceptionMetadata;

public class ExceptionSemanticExtractor {

  private static final Pattern EXCEPTION_PATTERN =
      Pattern.compile("WHEN\s+([A-Z_]+)\s+THEN", Pattern.CASE_INSENSITIVE);

  public List<ExceptionMetadata> extract(String sql) {

    List<ExceptionMetadata> exceptions = new ArrayList<>();

    Matcher matcher = EXCEPTION_PATTERN.matcher(sql);

    while (matcher.find()) {
      String exceptionName = matcher.group(1).toUpperCase();
      exceptions.add(new ExceptionMetadata(exceptionName, "OTHERS".equals(exceptionName)));
    }

    return exceptions;
  }

}
