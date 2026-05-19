package com.ignacio.legacyanalyzer.domain.services;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.ignacio.legacyanalyzer.domain.model.KnowledgeRelation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GraphRelationExtractor {

    private final SqlSemanticExtractor semanticExtractor;

    public List<KnowledgeRelation> extractKnowledgeRelations(
            String sourceCode,
            String objectName) {

        String normalized =
                sourceCode.toUpperCase();

        List<KnowledgeRelation> relations =
                new ArrayList<>();

        // =============================
        // CALLS
        // =============================

        Pattern callPattern =
                Pattern.compile(
                        "(\\w+)\\.(\\w+)\\s*\\(",
                        Pattern.CASE_INSENSITIVE);

        Matcher matcher =
                callPattern.matcher(sourceCode);

        while (matcher.find()) {

            String packageName =
                    matcher.group(1)
                            .toUpperCase();

            String procedureName =
                    matcher.group(2)
                            .toUpperCase();

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

        // =============================
        // READS
        // =============================

        List<String> readTables =
                semanticExtractor.extractReadTables(
                        normalized);

        for (String table : readTables) {

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

        // =============================
        // WRITES
        // =============================

        List<String> writeTables =
                semanticExtractor.extractWriteTables(
                        normalized);

        for (String table : writeTables) {

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

        // =============================
        // TRIGGER TARGET
        // =============================

        Pattern triggerPattern =
                Pattern.compile(
                        "\\bON\\s+([A-Z][A-Z0-9_.$#@]*)",
                        Pattern.CASE_INSENSITIVE);

        Matcher triggerMatcher =
                triggerPattern.matcher(sourceCode);

        if (normalized.contains("TRIGGER")
                && triggerMatcher.find()) {

            String targetTable =
                    triggerMatcher.group(1)
                            .toUpperCase();

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

        return relations
                .stream()
                .distinct()
                .toList();
    }
}