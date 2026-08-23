package com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface KnowledgeRelationRepository
        extends JpaRepository<KnowledgeRelationEntity, Long> {

boolean existsBySourceAndRelationAndTarget(
            String source,
            String relation,
            String target
    );

    boolean existsBySourceAndRelationAndTargetAndAnalysisId(
            String source,
            String relation,
            String target,
            String analysisId);

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


    @Query("""
    SELECT k.target,
           COUNT(k.target)
    FROM KnowledgeRelationEntity k
    WHERE k.relation = 'CALLS'
    GROUP BY k.target
    ORDER BY COUNT(k.target) DESC
""")
List<Object[]> findTopCallers();


}
