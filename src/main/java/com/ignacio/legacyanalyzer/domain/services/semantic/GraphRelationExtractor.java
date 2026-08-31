package com.ignacio.legacyanalyzer.domain.services.semantic;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import com.ignacio.legacyanalyzer.domain.model.KnowledgeRelation;
import com.ignacio.legacyanalyzer.domain.model.SubprogramNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GraphRelationExtractor {

    private static final double REGEX_CONFIDENCE = 0.8d;
    private final SqlSemanticExtractor semanticExtractor;

    public List<KnowledgeRelation> extractKnowledgeRelations(
            String sourceCode, String objectName, List<SubprogramNode> subprograms) {
        return extractKnowledgeRelations(sourceCode, objectName, subprograms,
                UUID.randomUUID().toString());
    }

    public List<KnowledgeRelation> extractKnowledgeRelations(
            String sourceCode, String objectName, List<SubprogramNode> subprograms,
            String analysisId) {

        String normalized = normalizePreservingOffsets(sourceCode);
        List<KnowledgeRelation> relations = new ArrayList<>();

        Matcher callMatcher = Pattern.compile(
                "\\b([A-Z][A-Z0-9_]*)\\.([A-Z][A-Z0-9_]*)\\s*\\(",
                Pattern.CASE_INSENSITIVE).matcher(normalized);

        while (callMatcher.find()) {
            String packageName = callMatcher.group(1).toUpperCase();
            if (!semanticExtractor.isValidCallTarget(packageName)
                    || packageName.equalsIgnoreCase(objectName)) {
                continue;
            }
            relations.add(relation(objectName, "CALLS", packageName,
                    objectName, sourceCode,
                    evidenceAround(sourceCode, callMatcher.start(), callMatcher.end()), analysisId));
        }

        for (String table : semanticExtractor.extractReadTables(normalized)) {
            relations.add(relation(objectName, "READS", table, objectName, sourceCode,
                    evidenceForTable(sourceCode, table, "READS"), analysisId));
        }

        for (String table : semanticExtractor.extractWriteTables(normalized)) {
            relations.add(relation(objectName, "WRITES", table, objectName, sourceCode,
                    evidenceForTable(sourceCode, table, "WRITES"), analysisId));
        }

        for (SubprogramNode subprogram : subprograms) {
            for (String call : subprogram.getCalls()) {
                String target = objectName + "." + call;
                if (semanticExtractor.isValidCallTarget(target)) {
                    relations.add(relation(subprogram.getQualifiedName(), "CALLS",
                            target, subprogram.getQualifiedName(), sourceCode,
                            evidenceForToken(sourceCode, call), analysisId));
                }
            }
        }

        Matcher triggerMatcher = Pattern.compile(
                "\\bON\\s+([A-Z][A-Z0-9_.$#@]*)", Pattern.CASE_INSENSITIVE)
                .matcher(normalized);
        if (normalized.contains("TRIGGER") && triggerMatcher.find()) {
            String targetTable = triggerMatcher.group(1).toUpperCase();
            if (semanticExtractor.isValidTableTarget(targetTable)) {
                relations.add(relation(objectName, "TRIGGER_ON", targetTable, objectName,
                        sourceCode,
                        evidenceAround(sourceCode, triggerMatcher.start(), triggerMatcher.end()),
                        analysisId));
            }
        }

        return relations.stream().distinct().toList();
    }

    private KnowledgeRelation relation(
            String source, String relation, String target, String sourceObject,
            String sourceCode, Evidence evidence, String analysisId) {

        Evidence resolved = evidence != null ? evidence : new Evidence(0, sourceCode.length());
        return new KnowledgeRelation(source, relation, target, sourceObject,
                lineAt(sourceCode, resolved.start()),
                lineAt(sourceCode, Math.max(resolved.start(), resolved.end() - 1)),
                sourceCode.substring(resolved.start(), resolved.end()),
                REGEX_CONFIDENCE, analysisId);
    }

    private Evidence evidenceForTable(String sourceCode, String table, String relation) {
        String keywords = relation.equals("WRITES")
                ? "(?:INSERT\\s+INTO|UPDATE|DELETE\\s+FROM|MERGE\\s+INTO)"
                : "(?:FROM|JOIN)";
        Matcher matcher = Pattern.compile(
                "(?i)\\b" + keywords + "\\s+" + Pattern.quote(table) + "\\b")
                .matcher(sourceCode);
        return matcher.find()
                ? evidenceAround(sourceCode, matcher.start(), matcher.end())
                : evidenceForToken(sourceCode, table);
    }

    private Evidence evidenceForToken(String sourceCode, String token) {
        Matcher matcher = Pattern.compile("(?i)\\b" + Pattern.quote(token) + "\\b")
                .matcher(sourceCode);
        return matcher.find() ? evidenceAround(sourceCode, matcher.start(), matcher.end()) : null;
    }

    private Evidence evidenceAround(String sourceCode, int matchStart, int matchEnd) {
        int start = sourceCode.lastIndexOf(';', Math.max(0, matchStart - 1));
        start = start < 0 ? 0 : start + 1;
        int end = sourceCode.indexOf(';', matchEnd);
        end = end < 0 ? sourceCode.length() : end + 1;
        while (start < end && Character.isWhitespace(sourceCode.charAt(start))) {
            start++;
        }
        while (end > start && Character.isWhitespace(sourceCode.charAt(end - 1))) {
            end--;
        }
        return new Evidence(start, end);
    }

    private int lineAt(String sourceCode, int offset) {
        int line = 1;
        for (int i = 0; i < offset && i < sourceCode.length(); i++) {
            if (sourceCode.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    private String normalizePreservingOffsets(String sourceCode) {
        char[] chars = sourceCode.toUpperCase().toCharArray();
        mask(chars, Pattern.compile("--[^\\r\\n]*").matcher(new String(chars)));
        mask(chars, Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL).matcher(new String(chars)));
        mask(chars, Pattern.compile("'(?:''|[^'])*'").matcher(new String(chars)));
        mask(chars, Pattern.compile("\\(\\+\\)").matcher(new String(chars)));
        return new String(chars);
    }

    private void mask(char[] chars, Matcher matcher) {
        while (matcher.find()) {
            for (int i = matcher.start(); i < matcher.end(); i++) {
                if (chars[i] != '\n' && chars[i] != '\r') {
                    chars[i] = ' ';
                }
            }
        }
    }

    private record Evidence(int start, int end) {
    }
}
