package com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class KnowledgeRelationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String source;

    private String relation;

    private String target;

    public KnowledgeRelationEntity() {
    }

    public KnowledgeRelationEntity(
            String source,
            String relation,
            String target
    ) {
        this.source = source;
        this.relation = relation;
        this.target = target;
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
}