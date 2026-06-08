package com.ignacio.legacyanalyzer.domain.services;

import java.util.List;
import org.springframework.stereotype.Service;
import com.ignacio.legacyanalyzer.application.dto.TopCallerResponse;
import com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence.KnowledgeRelationRepository;

@Service
public class TopCallersServiceImpl
        implements TopCallersService {

    private final KnowledgeRelationRepository repository;

    public TopCallersServiceImpl(
            KnowledgeRelationRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<TopCallerResponse> getTopCallers() {

        return repository.findTopCallers()
                .stream()
                .map(row -> new TopCallerResponse(
                        (String) row[0],
                        (Long) row[1]
                ))
                .toList();
    }
}