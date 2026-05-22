package com.ignacio.legacyanalyzer.domain.model;

import java.util.List;

public record CursorMetadata(

        String cursorName,

        List<String> referencedTables,

        boolean bulkCollect,

        boolean forUpdate,
        
        boolean forall

) {
}