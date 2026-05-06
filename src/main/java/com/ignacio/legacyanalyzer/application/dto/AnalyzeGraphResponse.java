package com.ignacio.legacyanalyzer.application.dto;

import java.util.List;
import java.util.Map;
import com.ignacio.legacyanalyzer.domain.model.SubprogramNode;

public record AnalyzeGraphResponse(

    String name,

    String type,

    List<String> nodes,

    List<Map<String, String>> edges,

    List<String> referencedTables,

    List<String> codeSmells,

    int riskScore,

    String riskLevel,

    String functionalSummary,

    List<SubprogramNode> subprograms

) {
}
