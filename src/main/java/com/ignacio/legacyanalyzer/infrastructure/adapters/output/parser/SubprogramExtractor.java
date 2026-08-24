package com.ignacio.legacyanalyzer.infrastructure.adapters.output.parser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.ignacio.legacyanalyzer.domain.model.SubprogramNode;
import com.ignacio.legacyanalyzer.domain.services.semantic.SqlSemanticExtractor;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SubprogramExtractor {

        private static final Pattern PROCEDURE_PATTERN =
                        Pattern.compile("(?is)PROCEDURE\\s+([A-Z0-9_]+).*?END\\s+\\1\\s*;");

        private static final Pattern FUNCTION_PATTERN =
                        Pattern.compile("(?is)FUNCTION\\s+([A-Z0-9_]+).*?END\\s+\\1\\s*;");

        private final SqlSemanticExtractor semanticExtractor;

        public SubprogramExtractor(SqlSemanticExtractor semanticExtractor) {
                this.semanticExtractor = semanticExtractor;
        }

        public List<SubprogramNode> extract(String sourceCode, String packageName) {

                List<SubprogramNode> result = new ArrayList<>();

                Set<String> knownSubprograms = extractKnownSubprograms(sourceCode);

                extractProcedures(sourceCode, packageName, result, knownSubprograms);

                extractFunctions(sourceCode, packageName, result, knownSubprograms);

                return result;
        }

        private void extractProcedures(String sourceCode, String packageName,
                        List<SubprogramNode> result, Set<String> knownSubprograms) {

                Matcher matcher = PROCEDURE_PATTERN.matcher(sourceCode);

                while (matcher.find()) {

                        String body = matcher.group();
                        String procedureName = matcher.group(1);

                        log.debug("Extracted procedure {} with body length {}",
                                        procedureName, body.length());

                        SubprogramNode node = new SubprogramNode();

                        node.setName(procedureName);

                        node.setQualifiedName(packageName + "." + procedureName);

                        node.setType("PROCEDURE");

                        node.setBody(body);

                        // =============================================
                        // SEMANTIC SQL EXTRACTION
                        // =============================================

                        node.setReads(semanticExtractor.extractReadTables(body));

                        node.setWrites(semanticExtractor.extractWriteTables(body));

                        // =============================================
                        // PROCEDURAL CALL GRAPH
                        // Modificado a ArrayList mutable para evitar el UnsupportedOperationException
                        // =============================================

                        node.setCalls(new ArrayList<>(extractCalls(body, knownSubprograms, procedureName)));

                        result.add(node);
                }
        }

        private void extractFunctions(String sourceCode, String packageName,
                        List<SubprogramNode> result, Set<String> knownSubprograms) {

                Matcher matcher = FUNCTION_PATTERN.matcher(sourceCode);

                while (matcher.find()) {

                        String body = matcher.group();

                        String functionName = matcher.group(1);

                        SubprogramNode node = new SubprogramNode();

                        node.setName(functionName);

                        node.setQualifiedName(packageName + "." + functionName);

                        node.setType("FUNCTION");

                        node.setBody(body);

                        // =============================================matcher.group();
                        // SEMANTIC SQL EXTRACTION
                        // =============================================

                        node.setReads(semanticExtractor.extractReadTables(body));

                        node.setWrites(semanticExtractor.extractWriteTables(body));

                        // =============================================
                        // PROCEDURAL CALL GRAPH (Sintaxis Arreglada)
                        // Modificado a ArrayList mutable usando functionName
                        // =============================================
                        
                        node.setCalls(new ArrayList<>(extractCalls(body, knownSubprograms, functionName)));

                        result.add(node);
                }
        }


        private Set<String> extractKnownSubprograms(String sourceCode) {

                Set<String> knownSubprograms = new HashSet<>();

                Matcher procedureMatcher = PROCEDURE_PATTERN.matcher(sourceCode);

                while (procedureMatcher.find()) {

                        knownSubprograms.add(procedureMatcher.group(1).toUpperCase());
                }

                Matcher functionMatcher = FUNCTION_PATTERN.matcher(sourceCode);

                while (functionMatcher.find()) {

                        knownSubprograms.add(functionMatcher.group(1).toUpperCase());
                }

                return knownSubprograms;
        }

        private List<String> extractCalls(String body, Set<String> knownSubprograms,
                        String currentSubprogram) {

                Set<String> calls = new HashSet<>();

                Pattern pattern = Pattern.compile("(\\w+)\\s*\\(", Pattern.CASE_INSENSITIVE);

                Matcher matcher = pattern.matcher(body);

                while (matcher.find()) {

                        String candidate = matcher.group(1).toUpperCase();

                        // =============================================
                        // IGNORE SELF CALL
                        // =============================================

                        if (candidate.equals(currentSubprogram.toUpperCase())) {

                                continue;
                        }

                        // =============================================
                        // ONLY VALID KNOWN SUBPROGRAMS
                        // =============================================

                        if (!knownSubprograms.contains(candidate)) {

                                continue;
            }

            calls.add(candidate);
        }

        return new ArrayList<>(calls);
    }
}
