package com.ignacio.legacyanalyzer.application.dto;

import java.util.Set;

public record ImpactAnalysisResponse(
        String rootObject,
        int impactSize,
        Set<String> impactedObjects
) {
}