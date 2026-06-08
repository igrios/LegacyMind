package com.ignacio.legacyanalyzer.domain.services;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import com.ignacio.legacyanalyzer.application.dto.ImpactAnalysisResponse;
import com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence.KnowledgeRelationEntity;
import com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence.KnowledgeRelationRepository;

@Service
public class ImpactAnalyzerService implements ImpactAnalyzer {

private final KnowledgeRelationRepository repository;

public ImpactAnalyzerService(
        KnowledgeRelationRepository repository) {
    this.repository = repository;
}

@Override
public ImpactAnalysisResponse analyze(
        String objectName) {

    Set<String> visited = new HashSet<>();
    Set<String> impacted = new HashSet<>();

    traverse(
            objectName,
            visited,
            impacted
    );

    System.out.println("=================================");
        System.out.println("ROOT: " + objectName);
        System.out.println("IMPACT SIZE: " + impacted.size());

        impacted.forEach(System.out::println);

        System.out.println("=================================");

     

    return new ImpactAnalysisResponse(
            objectName,
            impacted.size(),
            impacted
    );
}

private void traverse(
        String current,
        Set<String> visited,
        Set<String> impacted) {

    if (!visited.add(current)) {
        return;
    }

    List<KnowledgeRelationEntity> calls =
            repository.findBySourceAndRelation(
                    current,
                    "CALLS"
            );

    for (KnowledgeRelationEntity call : calls) {

        String target = call.getTarget();

        impacted.add(target);

        traverse(
                target,
                visited,
                impacted
        );
    }
}


}
