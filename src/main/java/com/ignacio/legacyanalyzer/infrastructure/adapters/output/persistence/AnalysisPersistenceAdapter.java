package com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence;

import java.util.List;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Repository;
import com.ignacio.legacyanalyzer.application.mapper.LegacyObjectMapper;
import com.ignacio.legacyanalyzer.domain.model.LegacyObject;
import com.ignacio.legacyanalyzer.domain.model.TableDependency;
import com.ignacio.legacyanalyzer.domain.ports.PersistAnalysisPort;
import com.ignacio.legacyanalyzer.domain.ports.TableDependencyRepositoryPort;

@Repository
public class AnalysisPersistenceAdapter implements PersistAnalysisPort {

    private final LegacyObjectRepository legacyObjectRepository;
    private final KnowledgeRelationRepository knowledgeRelationRepository;
    private final TableDependencyRepositoryPort dependencyRepository;
    private final LegacyObjectMapper mapper = new LegacyObjectMapper();

    public AnalysisPersistenceAdapter(
            LegacyObjectRepository legacyObjectRepository,
            KnowledgeRelationRepository knowledgeRelationRepository,
            TableDependencyRepositoryPort dependencyRepository) {

        this.legacyObjectRepository = legacyObjectRepository;
        this.knowledgeRelationRepository = knowledgeRelationRepository;
        this.dependencyRepository = dependencyRepository;
    }

    @Override
    public void persist(LegacyObject legacyObject, List<TableDependency> dependencies) {
        Set<String> processedIdentities = new HashSet<>();
        legacyObject.getKnowledgeRelations().forEach(relation -> {
            String identity = String.join("\u001f",
                    relation.source(), relation.relation(), relation.target(),
                    String.valueOf(relation.analysisId()));
            if (!processedIdentities.add(identity)) {
                return;
            }

            boolean exists = knowledgeRelationRepository
                    .existsBySourceAndRelationAndTargetAndAnalysisId(
                            relation.source(), relation.relation(), relation.target(),
                            relation.analysisId());

            if (!exists) {
                knowledgeRelationRepository.save(new KnowledgeRelationEntity(
                        relation.source(), relation.relation(), relation.target(),
                        relation.sourceObject(), relation.sourceLineStart(),
                        relation.sourceLineEnd(), relation.codeSnippet(),
                        relation.confidenceLevel(), relation.analysisId()));
            }
        });

        dependencyRepository.saveAll(dependencies);
        legacyObjectRepository.save(mapper.toEntity(legacyObject));
    }
}
