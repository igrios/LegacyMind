package com.ignacio.legacyanalyzer.application.dto;

import java.util.List;
import com.ignacio.legacyanalyzer.domain.model.BusinessRuleMetadata;
import com.ignacio.legacyanalyzer.domain.model.CursorMetadata;
import com.ignacio.legacyanalyzer.domain.model.ExceptionMetadata;
import com.ignacio.legacyanalyzer.domain.model.KnowledgeRelation;
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

                List<SubprogramNode> subprograms, List<CursorMetadata> cursors,

                List<ExceptionMetadata> exceptions, List<BusinessRuleMetadata> businessRules,

                List<KnowledgeRelation> knowledgeRelations

) {
}
