package com.ignacio.legacyanalyzer.infrastructure.adapters.input.controller;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ignacio.legacyanalyzer.application.dto.AnalyzeLegacyRequest;
import com.ignacio.legacyanalyzer.application.dto.AnalyzeLegacyResponse;
import com.ignacio.legacyanalyzer.domain.model.LegacyObject;
import com.ignacio.legacyanalyzer.infrastructure.adapters.output.parser.RegexLegacyParserAdapter;
import com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence.LegacyObjectEntity;
import com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence.LegacyObjectRepository;


@RestController
@RequestMapping("/api/legacy")
public class LegacyController {

        private final RegexLegacyParserAdapter parserAdapter;
        private final LegacyObjectRepository repository;

        public LegacyController(LegacyObjectRepository repository) {
                this.parserAdapter = new RegexLegacyParserAdapter();
                this.repository = repository;
        }

        @PostMapping("/analyze")
        public AnalyzeLegacyResponse analyze(@RequestBody AnalyzeLegacyRequest request) {

                LegacyObject object = parserAdapter.parse(request.getSourceCode());

                LegacyObjectEntity entity = new LegacyObjectEntity(object.getId(), object.getName(),
                                object.getType(), object.getSourceCode(),
                                String.join(",", object.getProcedures()),
                                String.join(",", object.getReferencedTables()),
                                String.join(",", object.getCodeSmells()), object.getRiskScore(),
                                object.getRiskLevel(), LocalDateTime.now());

                repository.save(entity);

                return new AnalyzeLegacyResponse(object.getName(), object.getType(),
                                object.getProcedures(), object.getReferencedTables(),
                                object.getCodeSmells(), object.getRiskScore(), object.getRiskLevel()


                );
        }

        @GetMapping("/history")
        public List<AnalyzeLegacyResponse> history() {

                return repository.findAll().stream().map(entity -> new AnalyzeLegacyResponse(
                                entity.getName(), entity.getType(),

                                entity.getProcedures() != null
                                                ? List.of(entity.getProcedures().split(","))
                                                : List.of(),

                                entity.getReferencedTables() != null
                                                ? List.of(entity.getReferencedTables().split(","))
                                                : List.of(),

                                entity.getCodeSmells() != null
                                                ? List.of(entity.getCodeSmells().split(","))
                                                : List.of(),

                                entity.getRiskScore() != null ? entity.getRiskScore() : 0,

                                entity.getRiskLevel() != null ? entity.getRiskLevel() : "LOW"))
                                .toList();
        }


}
