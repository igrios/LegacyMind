package com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(
        name = "uk_knowledge_relation_identity",
        columnNames = {"source", "relation", "target", "analysis_id"}))
public class KnowledgeRelationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String source;

    private String relation;

    private String target;

    @Column(name = "source_object")
    private String sourceObject;

    @Column(name = "source_line_start")
    private Integer sourceLineStart;

    @Column(name = "source_line_end")
    private Integer sourceLineEnd;

    @Column(name = "code_snippet", columnDefinition = "TEXT")
    private String codeSnippet;

    @Column(name = "confidence_level")
    private Double confidenceLevel;

    @Column(name = "analysis_id")
    private String analysisId;

    public KnowledgeRelationEntity() {
    }

    public KnowledgeRelationEntity(
            String source,
            String relation,
            String target,
            String sourceObject,
            Integer sourceLineStart,
            Integer sourceLineEnd,
            String codeSnippet,
            Double confidenceLevel,
            String analysisId
    ) {
        this.source = source;
        this.relation = relation;
        this.target = target;
        this.sourceObject = sourceObject;
        this.sourceLineStart = sourceLineStart;
        this.sourceLineEnd = sourceLineEnd;
        this.codeSnippet = codeSnippet;
        this.confidenceLevel = confidenceLevel;
        this.analysisId = analysisId;
    }

    public Long getId() {
        return id;
    }

    public String getSource() {
        return source;
    }

    public String getRelation() {
        return relation;
    }

    public String getTarget() {
        return target;
    }

    public String getSourceObject() {
        return sourceObject;
    }

    public Integer getSourceLineStart() {
        return sourceLineStart;
    }

    public Integer getSourceLineEnd() {
        return sourceLineEnd;
    }

    public String getCodeSnippet() {
        return codeSnippet;
    }

    public Double getConfidenceLevel() {
        return confidenceLevel;
    }

    public String getAnalysisId() {
        return analysisId;
    }
}
