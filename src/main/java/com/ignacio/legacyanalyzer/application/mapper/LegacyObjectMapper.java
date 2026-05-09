package com.ignacio.legacyanalyzer.application.mapper;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import com.ignacio.legacyanalyzer.application.dto.AnalyzeLegacyResponse;
import com.ignacio.legacyanalyzer.domain.model.LegacyObject;
import com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence.LegacyObjectEntity;

public class LegacyObjectMapper {

    public LegacyObjectEntity toEntity(LegacyObject object) {

        return new LegacyObjectEntity(

                object.getId(),

                object.getName(),

                object.getType(),

                object.getSourceCode(),

                String.join(",", object.getProcedures()),

                String.join(",", object.getReferencedTables()),

                String.join(",", object.getCodeSmells()),

                object.getRiskScore(),

                object.getRiskLevel(),

                object.getFunctionalSummary(),

                LocalDateTime.now());
    }

    public AnalyzeLegacyResponse toResponse(LegacyObject object) {

        return new AnalyzeLegacyResponse(

                object.getName(),

                object.getType(),

                object.getProcedures(),

                object.getReferencedTables(),

                object.getCodeSmells(),

                object.getRiskScore(),

                object.getRiskLevel(),

                object.getFunctionalSummary(),

                object.getSubprograms(),

                object.getKnowledgeRelations());
    }

    public AnalyzeLegacyResponse toResponse(LegacyObjectEntity entity) {

        return new AnalyzeLegacyResponse(

                entity.getName(),

                entity.getType(),

                split(entity.getProcedures()),

                split(entity.getReferencedTables()),

                split(entity.getCodeSmells()),

                entity.getRiskScore() != null ? entity.getRiskScore() : 0,

                entity.getRiskLevel() != null ? entity.getRiskLevel() : "LOW",

                entity.getFunctionalSummary() != null ? entity.getFunctionalSummary()
                        : "No summary available",

                List.of(),

                List.of());
    }

    private List<String> split(String value) {

        if (value == null || value.isBlank()) {
            return List.of();
        }

        return Arrays.stream(value.split(",")).map(String::trim).toList();
    }
}
