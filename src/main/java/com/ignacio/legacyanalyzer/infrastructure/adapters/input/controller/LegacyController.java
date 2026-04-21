package com.ignacio.legacyanalyzer.infrastructure.adapters.input.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ignacio.legacyanalyzer.application.dto.AnalyzeLegacyRequest;
import com.ignacio.legacyanalyzer.application.dto.AnalyzeLegacyResponse;
import com.ignacio.legacyanalyzer.domain.model.LegacyObject;
import com.ignacio.legacyanalyzer.infrastructure.adapters.output.parser.RegexLegacyParserAdapter;

@RestController
@RequestMapping("/api/legacy")
public class LegacyController {

    private final RegexLegacyParserAdapter parserAdapter;

    public LegacyController() {
        this.parserAdapter = new RegexLegacyParserAdapter();
    }

    @PostMapping("/analyze")
    public AnalyzeLegacyResponse analyze(@RequestBody AnalyzeLegacyRequest request) {

        LegacyObject object = parserAdapter.parse(request.getSourceCode());

        return new AnalyzeLegacyResponse(object.getName(), object.getType(), object.getProcedures(),
                object.getReferencedTables());
    }
}
