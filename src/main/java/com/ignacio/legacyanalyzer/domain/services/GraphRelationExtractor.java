package com.ignacio.legacyanalyzer.domain.services;

import java.util.ArrayList;
import java.util.List;
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

    private final SqlSemanticExtractor semanticExtractor;

    public List<KnowledgeRelation> extractKnowledgeRelations(
        String sourceCode,
        String objectName,
        List<SubprogramNode> subprograms){

        // =============================================
        // NORMALIZATION
        // =============================================

        String normalized =
                sourceCode.toUpperCase();

        // =============================================
        // REMOVE COMMENTS
        // =============================================

        normalized =
                normalized.replaceAll(
                        "--.*?(\\r?\\n|$)",
                        " ");

        normalized =
                normalized.replaceAll(
                        "/\\*.*?\\*/",
                        " ");

        // =============================================
        // REMOVE ORACLE OUTER JOIN (+)
        // =============================================

        normalized =
                normalized.replaceAll(
                        "\\(\\+\\)",
                        "");

        List<KnowledgeRelation> relations =
                new ArrayList<>();

        // =============================================
        // CALLS
        // =============================================

        Pattern callPattern =
                Pattern.compile(
                        "\\b([A-Z][A-Z0-9_]*)\\.([A-Z][A-Z0-9_]*)\\s*\\(",
                        Pattern.CASE_INSENSITIVE);

        Matcher matcher =
                callPattern.matcher(normalized);

        while (matcher.find()) {

            String packageName =
                    matcher.group(1)
                            .toUpperCase();

            String procedureName =
                    matcher.group(2)
                            .toUpperCase();

            // =========================================
            // IGNORE INVALID SHORT TOKENS
            // =========================================

            if (packageName.length() < 3) {

                log.warn(
                        "IGNORING INVALID CALL TOKEN >>> {}",
                        packageName);

                continue;
            }

            // =========================================
            // IGNORE SELF REFERENCES
            // =========================================

            if (packageName.equals(objectName)) {

                continue;
            }

            relations.add(

                    new KnowledgeRelation(
                            objectName,
                            "CALLS",
                            packageName));

            log.debug(
                    "CALL DETECTED >>> {} -> {}.{}",
                    objectName,
                    packageName,
                    procedureName);
        }

        // =============================================
        // READS
        // =============================================

        List<String> readTables =
                semanticExtractor.extractReadTables(
                        normalized);

        for (String table : readTables) {

            if (!isValidSemanticObject(table)) {

                continue;
            }

            relations.add(

                    new KnowledgeRelation(
                            objectName,
                            "READS",
                            table));

            log.debug(
                    "READ DETECTED >>> {} -> {}",
                    objectName,
                    table);
        }

        // =============================================
        // WRITES
        // =============================================

        List<String> writeTables =
                semanticExtractor.extractWriteTables(
                        normalized);

        for (String table : writeTables) {

            if (!isValidSemanticObject(table)) {

                continue;
            }

            relations.add(

                    new KnowledgeRelation(
                            objectName,
                            "WRITES",
                            table));

            log.debug(
                    "WRITE DETECTED >>> {} -> {}",
                    objectName,
                    table);
        }

for (SubprogramNode subprogram : subprograms) {

    for (String call : subprogram.getCalls()) {

        relations.add(

                new KnowledgeRelation(

                        subprogram.getQualifiedName(),

                        "CALLS",

                        objectName + "." + call
                )
        );

        log.debug(
                "CALL DETECTED >>> {} -> {}",
                subprogram.getQualifiedName(),
                call
        );
    }
}




        // =============================================
        // TRIGGER TARGET
        // =============================================

        Pattern triggerPattern =
                Pattern.compile(
                        "\\bON\\s+([A-Z][A-Z0-9_.$#@]*)",
                        Pattern.CASE_INSENSITIVE);

        Matcher triggerMatcher =
                triggerPattern.matcher(normalized);

        if (normalized.contains("TRIGGER")
                && triggerMatcher.find()) {

            String targetTable =
                    triggerMatcher.group(1)
                            .toUpperCase();

            if (isValidSemanticObject(targetTable)) {

                relations.add(

                        new KnowledgeRelation(
                                objectName,
                                "TRIGGER_ON",
                                targetTable));

                log.debug(
                        "TRIGGER DETECTED >>> {} -> {}",
                        objectName,
                        targetTable);
            }
        }

        return relations
                .stream()
                .distinct()
                .toList();
    }

    // =============================================
    // VALIDATION
    // =============================================

    private boolean isValidSemanticObject(String value) {

        if (value == null || value.isBlank()) {

            return false;
        }

        value =
                value.toUpperCase()
                        .trim();

        // =========================================
        // IGNORE SHORT TOKENS
        // =========================================

        if (value.length() < 3) {

            return false;
        }

        // =========================================
        // IGNORE SQL KEYWORDS
        // =========================================

        return !List.of(
                "SELECT",
                "FROM",
                "WHERE",
                "JOIN",
                "LEFT",
                "RIGHT",
                "INNER",
                "OUTER",
                "ON",
                "AND",
                "OR",
                "END",
                "IS",
                "AS",
                "BY",
                "IN",
                "TO")
                .contains(value);
    }
}