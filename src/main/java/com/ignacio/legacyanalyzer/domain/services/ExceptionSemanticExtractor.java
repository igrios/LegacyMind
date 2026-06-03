package com.ignacio.legacyanalyzer.domain.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.ignacio.legacyanalyzer.domain.model.ExceptionMetadata;

public class ExceptionSemanticExtractor {

    private static final Pattern EXCEPTION_PATTERN =
            Pattern.compile(
                    "WHEN\\s+([A-Z_]+)\\s+THEN",
                    Pattern.CASE_INSENSITIVE);

    private static final Set<String> NON_EXCEPTIONS =
            Set.of(
                    "MATCHED",
                    "MERGED");

    public List<ExceptionMetadata> extract(String sql) {

        List<ExceptionMetadata> exceptions =
                new ArrayList<>();

        String normalized =
                sql.toUpperCase();

        int exceptionIndex =
                normalized.indexOf("EXCEPTION");

        if (exceptionIndex == -1) {

            return exceptions;
        }

        String exceptionBlock =
                normalized.substring(exceptionIndex);

        Matcher matcher =
                EXCEPTION_PATTERN.matcher(exceptionBlock);

        while (matcher.find()) {

            String exceptionName =
                    matcher.group(1).toUpperCase();

            if (NON_EXCEPTIONS.contains(exceptionName)) {

                continue;
            }

            exceptions.add(

                    new ExceptionMetadata(

                            exceptionName,

                            "OTHERS".equals(exceptionName)));
        }

        return exceptions;
    }
}