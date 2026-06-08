package com.ignacio.legacyanalyzer.application.dto;

public record TopCallerResponse(
        String objectName,
        Long callCount
) {
}
