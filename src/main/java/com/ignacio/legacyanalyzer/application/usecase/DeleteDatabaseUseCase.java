package com.ignacio.legacyanalyzer.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence.KnowledgeRelationRepository;
import com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence.LegacyObjectRepository;
import com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence.TableDependencyJpaRepository;

@Service
public class DeleteDatabaseUseCase {

    private final TableDependencyJpaRepository
            tableDependencyRepository;

    private final LegacyObjectRepository
            legacyObjectRepository;

    private final KnowledgeRelationRepository
            knowledgeRelationRepository;

    public DeleteDatabaseUseCase(
            TableDependencyJpaRepository tableDependencyRepository,
            LegacyObjectRepository legacyObjectRepository,
            KnowledgeRelationRepository knowledgeRelationRepository) {

        this.tableDependencyRepository =
                tableDependencyRepository;

        this.legacyObjectRepository =
                legacyObjectRepository;

        this.knowledgeRelationRepository =
                knowledgeRelationRepository;
    }

    @Transactional
    public void execute() {

        knowledgeRelationRepository.deleteAll();

        tableDependencyRepository.deleteAll();

        legacyObjectRepository.deleteAll();
    }
}