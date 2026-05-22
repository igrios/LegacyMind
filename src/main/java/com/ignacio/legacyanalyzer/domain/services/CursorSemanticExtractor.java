package com.ignacio.legacyanalyzer.domain.services;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import com.ignacio.legacyanalyzer.domain.model.CursorMetadata;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CursorSemanticExtractor {

    // =============================================
    // CURSOR PATTERN
    // =============================================

    private static final Pattern CURSOR_PATTERN =
            Pattern.compile(
                    "\\bCURSOR\\s+(\\w+)\\s+IS",
                    Pattern.CASE_INSENSITIVE);

    // =============================================
    // BULK COLLECT PATTERN
    // =============================================

    private static final Pattern BULK_COLLECT_PATTERN =
            Pattern.compile(
                    "\\bBULK\\s+COLLECT\\b",
                    Pattern.CASE_INSENSITIVE);

    // =============================================
    // FORALL PATTERN
    // =============================================

    private static final Pattern FORALL_PATTERN =
            Pattern.compile(
                    "\\bFORALL\\b",
                    Pattern.CASE_INSENSITIVE);

    // =============================================
    // FOR UPDATE PATTERN
    // =============================================

    private static final Pattern FOR_UPDATE_PATTERN =
            Pattern.compile(
                    "\\bFOR\\s+UPDATE\\b",
                    Pattern.CASE_INSENSITIVE);

    // =============================================
    // TABLE PATTERN
    // =============================================

    private static final Pattern FROM_PATTERN =
            Pattern.compile(
                    "\\bFROM\\s+([A-Z][A-Z0-9_]*)",
                    Pattern.CASE_INSENSITIVE);

    // =============================================
    // EXTRACT CURSORS
    // =============================================

    public List<CursorMetadata> extractCursors(
            String sourceCode) {

        List<CursorMetadata> cursors =
                new ArrayList<>();

        String normalized =
                sourceCode.toUpperCase();

        Matcher matcher =
                CURSOR_PATTERN.matcher(normalized);

        while (matcher.find()) {

            String cursorName =
                    matcher.group(1);

            log.debug(
                    "CURSOR DETECTED >>> {}",
                    cursorName);

            // =====================================
            // REFERENCED TABLES
            // =====================================

            List<String> tables =
                    extractCursorTables(normalized);

            // =====================================
            // BULK COLLECT
            // =====================================

            boolean bulkCollect =
                    BULK_COLLECT_PATTERN
                            .matcher(normalized)
                            .find();

            // =====================================
            // FOR UPDATE
            // =====================================

            boolean forUpdate =
                    FOR_UPDATE_PATTERN
                            .matcher(normalized)
                            .find();

            // =====================================
            // FORALL
            // =====================================

            boolean forall =
                    FORALL_PATTERN
                            .matcher(normalized)
                            .find();

            CursorMetadata metadata =
                    new CursorMetadata(
                            cursorName,
                            tables,
                            bulkCollect,
                            forUpdate,
                            forall);

            cursors.add(metadata);

            log.debug(
                    "CURSOR METADATA >>> {}",
                    metadata);
        }

        return cursors;
    }

    // =============================================
    // EXTRACT TABLES
    // =============================================

    private List<String> extractCursorTables(
            String sql) {

        List<String> tables =
                new ArrayList<>();

        Matcher matcher =
                FROM_PATTERN.matcher(sql);

        while (matcher.find()) {

            String table =
                    matcher.group(1);

            if (!tables.contains(table)) {

                tables.add(table);

                log.debug(
                        "CURSOR TABLE >>> {}",
                        table);
            }
        }

        return tables;
    }
}