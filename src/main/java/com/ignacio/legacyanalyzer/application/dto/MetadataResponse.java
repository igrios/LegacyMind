
package com.ignacio.legacyanalyzer.application.dto;

import java.util.List;
import com.ignacio.legacyanalyzer.domain.model.BusinessRuleMetadata;
import com.ignacio.legacyanalyzer.domain.model.CursorMetadata;
import com.ignacio.legacyanalyzer.domain.model.DbLinkMetadata;
import com.ignacio.legacyanalyzer.domain.model.ExceptionMetadata;

public record MetadataResponse(

    String name,

    String type,

    List<CursorMetadata> cursors,

    List<ExceptionMetadata> exceptions,

    List<BusinessRuleMetadata> businessRules,

    List<DbLinkMetadata> dbLinks

) {
}