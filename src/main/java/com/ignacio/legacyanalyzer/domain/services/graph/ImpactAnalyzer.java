package com.ignacio.legacyanalyzer.domain.services.graph;

import com.ignacio.legacyanalyzer.application.dto.ImpactAnalysisResponse;

public interface ImpactAnalyzer {


    ImpactAnalysisResponse analyze(
            String objectName);

}
