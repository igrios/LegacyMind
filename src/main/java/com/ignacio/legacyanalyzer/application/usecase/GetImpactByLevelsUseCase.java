package com.ignacio.legacyanalyzer.application.usecase;

import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import com.ignacio.legacyanalyzer.domain.services.ImpactAnalysisService;

@Service
public class GetImpactByLevelsUseCase {

    private final ImpactAnalysisService impactService;

    public GetImpactByLevelsUseCase(ImpactAnalysisService impactService) {
        this.impactService = impactService;
    }

    public Map<Integer, Set<String>> execute(String table) {
        return impactService.getImpactByLevels(table);
    }
}