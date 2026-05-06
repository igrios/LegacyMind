package com.ignacio.legacyanalyzer.infrastructure.adapters.output.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ignacio.legacyanalyzer.domain.model.SubprogramNode;

public class SubprogramExtractor {

    private static final Pattern PROCEDURE_PATTERN =
            Pattern.compile(
                    "(?is)PROCEDURE\\s+([A-Z0-9_]+).*?END\\s+\\1\\s*;"
            );

    private static final Pattern FUNCTION_PATTERN =
            Pattern.compile(
                    "(?is)FUNCTION\\s+([A-Z0-9_]+).*?END\\s+\\1\\s*;"
            );

    public List<SubprogramNode> extract(
            String sourceCode,
            String packageName
    ) {

        List<SubprogramNode> result =
                new ArrayList<>();

        extractProcedures(
                sourceCode,
                packageName,
                result
        );

        extractFunctions(
                sourceCode,
                packageName,
                result
        );

        return result;
    }

    private void extractProcedures(
            String sourceCode,
            String packageName,
            List<SubprogramNode> result
    ) {

        Matcher matcher =
                PROCEDURE_PATTERN.matcher(
                        sourceCode
                );

        while (matcher.find()) {

            SubprogramNode node =
                    new SubprogramNode();

            String procedureName =
                    matcher.group(1);

            node.setName(
                    procedureName
            );

            node.setQualifiedName(
                    packageName + "." + procedureName
            );

            node.setType(
                    "PROCEDURE"
            );

            node.setBody(
                    matcher.group()
            );

            result.add(node);
        }
    }

    private void extractFunctions(
            String sourceCode,
            String packageName,
            List<SubprogramNode> result
    ) {

        Matcher matcher =
                FUNCTION_PATTERN.matcher(
                        sourceCode
                );

        while (matcher.find()) {

            SubprogramNode node =
                    new SubprogramNode();

            String functionName =
                    matcher.group(1);

            node.setName(
                    functionName
            );

            node.setQualifiedName(
                    packageName + "." + functionName
            );

            node.setType(
                    "FUNCTION"
            );

            node.setBody(
                    matcher.group()
            );

            result.add(node);
        }
    }
}