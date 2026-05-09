package com.ignacio.legacyanalyzer.infrastructure.adapters.output.parser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

            String body =
                    matcher.group();

            String procedureName =
                    matcher.group(1);

            SubprogramNode node =
                    new SubprogramNode();

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
                    body
            );

            node.setReads(
                    extractReads(body)
            );

            node.setWrites(
                    extractWrites(body)
            );

            node.setCalls(
                    extractCalls(body)
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

            String body =
                    matcher.group();

            String functionName =
                    matcher.group(1);

            SubprogramNode node =
                    new SubprogramNode();

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
                    body
            );

            node.setReads(
                    extractReads(body)
            );

            node.setWrites(
                    extractWrites(body)
            );

            node.setCalls(
                    extractCalls(body)
            );

            result.add(node);
        }
    }

    private List<String> extractReads(String body) {

        Set<String> reads =
                new HashSet<>();

        Pattern pattern =
                Pattern.compile(
                        "\\bFROM\\s+(\\w+)",
                        Pattern.CASE_INSENSITIVE
                );

        Matcher matcher =
                pattern.matcher(body);

        while (matcher.find()) {

            reads.add(
                    matcher.group(1).toUpperCase()
            );
        }

        return reads.stream().toList();
    }

    private List<String> extractWrites(String body) {

        Set<String> writes =
                new HashSet<>();

        Pattern updatePattern =
                Pattern.compile(
                        "\\bUPDATE\\s+(\\w+)",
                        Pattern.CASE_INSENSITIVE
                );

        Pattern insertPattern =
                Pattern.compile(
                        "\\bINSERT\\s+INTO\\s+(\\w+)",
                        Pattern.CASE_INSENSITIVE
                );

        Matcher updateMatcher =
                updatePattern.matcher(body);

        while (updateMatcher.find()) {

            writes.add(
                    updateMatcher.group(1).toUpperCase()
            );
        }

        Matcher insertMatcher =
                insertPattern.matcher(body);

        while (insertMatcher.find()) {

            writes.add(
                    insertMatcher.group(1).toUpperCase()
            );
        }

        return writes.stream().toList();
    }

    private List<String> extractCalls(String body) {

        Set<String> calls =
                new HashSet<>();

        Pattern pattern =
                Pattern.compile(
                        "(\\w+)\\.(\\w+)\\s*\\(",
                        Pattern.CASE_INSENSITIVE
                );

        Matcher matcher =
                pattern.matcher(body);

        while (matcher.find()) {

            calls.add(
                    matcher.group(1).toUpperCase()
            );
        }

        return calls.stream().toList();
    }
}