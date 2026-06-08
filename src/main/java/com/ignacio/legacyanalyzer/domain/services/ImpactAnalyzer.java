package com.ignacio.legacyanalyzer.domain.services;

import com.ignacio.legacyanalyzer.application.dto.ImpactAnalysisResponse;

public interface ImpactAnalyzer {


    ImpactAnalysisResponse analyze(
            String objectName);

}
