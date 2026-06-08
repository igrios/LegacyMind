package com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeRelationRepository
        extends JpaRepository<KnowledgeRelationEntity, Long> {

boolean existsBySourceAndRelationAndTarget(
            String source,
            String relation,
            String target
    );

    List<KnowledgeRelationEntity> findAll();

    List<KnowledgeRelationEntity>
    findBySourceAndRelation(
            String source,
            String relation
    );

    List<KnowledgeRelationEntity>
    findByTargetAndRelation(
            String target,
            String relation
    );


}