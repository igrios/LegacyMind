package com.ignacio.legacyanalyzer.application.usecase;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.ignacio.legacyanalyzer.domain.services.ImpactAnalysisService;

@Service
public class GetImpactUseCase {

    private final ImpactAnalysisService impactService;

    
    public GetImpactUseCase(ImpactAnalysisService impactService) {
          this.impactService = impactService;
    }

   public Set<String> execute(String table) {
    Set<String> result = impactService.getImpact(table);

    return result.stream()
            .sorted()
            .collect(Collectors.toCollection(LinkedHashSet::new));
}
}