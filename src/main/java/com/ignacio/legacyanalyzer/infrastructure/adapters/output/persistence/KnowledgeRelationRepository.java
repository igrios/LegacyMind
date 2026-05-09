package com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KnowledgeRelationRepository
        extends JpaRepository<KnowledgeRelationEntity, Long> {


           boolean existsBySourceAndRelationAndTarget(
            String source,
            String relation,
            String target
    );

List<KnowledgeRelationEntity> findAll();


}