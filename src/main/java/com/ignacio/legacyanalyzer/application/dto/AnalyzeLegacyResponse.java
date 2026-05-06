package com.ignacio.legacyanalyzer.application.dto;

import java.util.List;

import com.ignacio.legacyanalyzer.domain.model.SubprogramNode;

public record AnalyzeLegacyResponse(

        String name,

        String type,

        List<String> procedures,

        List<String> referencedTables,

        List<String> codeSmells,

        int riskScore,

        String riskLevel,

        String functionalSummary,

        List<SubprogramNode> subprograms

) {
}