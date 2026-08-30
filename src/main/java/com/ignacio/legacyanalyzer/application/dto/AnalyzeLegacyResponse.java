package com.ignacio.legacyanalyzer.application.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ignacio.legacyanalyzer.domain.model.BusinessRuleMetadata;
import com.ignacio.legacyanalyzer.domain.model.CursorMetadata;
import com.ignacio.legacyanalyzer.domain.model.DbLinkMetadata;
import com.ignacio.legacyanalyzer.domain.model.ExceptionMetadata;
import com.ignacio.legacyanalyzer.domain.model.KnowledgeRelation;
import com.ignacio.legacyanalyzer.domain.model.SubprogramNode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AnalyzeLegacyResponse {

    private String name;
    private String type;
    private List<String> procedures;
    private List<String> referencedTables;
    private List<String> codeSmells;
    private int riskScore;
    private String riskLevel;
    private String functionalSummary;
    private List<SubprogramNode> subprograms;
    private List<CursorMetadata> cursors;
    private List<ExceptionMetadata> exceptions;
    private List<DbLinkMetadata> dbLinks;
    private List<BusinessRuleMetadata> businessRules;

    @JsonProperty("knowledgeRelations")
    private List<KnowledgeRelation> knowledgeRelations;
}
