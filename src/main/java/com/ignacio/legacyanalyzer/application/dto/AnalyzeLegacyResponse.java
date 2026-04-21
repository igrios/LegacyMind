package com.ignacio.legacyanalyzer.application.dto;

import java.util.List;

public record AnalyzeLegacyResponse(
        String name,
        String type,
        List<String> procedures,
        List<String> referencedTables
) {
}