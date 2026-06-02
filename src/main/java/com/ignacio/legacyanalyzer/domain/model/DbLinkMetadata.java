package com.ignacio.legacyanalyzer.domain.model;

// Metadata para referencias a DB Links en el código SQL


public record DbLinkMetadata(

        String tableName,

        String dbLinkName

) {
}