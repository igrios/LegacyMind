package com.ignacio.legacyanalyzer.application.mapper;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import com.ignacio.legacyanalyzer.application.dto.AnalyzeLegacyResponse;
import com.ignacio.legacyanalyzer.domain.model.BusinessRuleMetadata;
import com.ignacio.legacyanalyzer.domain.model.CursorMetadata;
import com.ignacio.legacyanalyzer.domain.model.LegacyObject;
import com.ignacio.legacyanalyzer.domain.model.SubprogramNode;
import com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence.LegacyObjectEntity;
import com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence.entity.BusinessRuleEntity;
import com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence.entity.CursorMetadataEntity;
import com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence.entity.SubprogramNodeEntity;



public class LegacyObjectMapper {

        public LegacyObjectEntity toEntity(LegacyObject object) {

                // =========================================
                // MAP CURSORS FIRST
                // =========================================

                List<CursorMetadataEntity> cursorEntities = mapCursors(object.getCursors());
                List<SubprogramNodeEntity> subprogramEntities = mapSubprograms(object.getSubprograms());
               List<BusinessRuleEntity> businessRuleEntities =  mapBusinessRules(object.getBusinessRules());
                // =========================================
                // CREATE PARENT ENTITY
                // =========================================

                LegacyObjectEntity entity = new LegacyObjectEntity(

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

                                cursorEntities,

                                businessRuleEntities,

                                subprogramEntities,

                                LocalDateTime.now());

                // =========================================
                // SET PARENT REFERENCE
                // =========================================

                cursorEntities.forEach(

                                cursor -> cursor.setLegacyObject(entity));

                System.out.println("CURSOR ENTITIES SIZE >>> " + cursorEntities.size());

                cursorEntities.forEach(

                                c -> System.out.println("CURSOR ENTITY >>> " + c.getCursorName()));
                return entity;
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

                                entity.getFunctionalSummary() != null
                                                ? entity.getFunctionalSummary()
                                                : "No summary available",

                                List.of(),

                                List.of());
        }

        private List<String> split(String value) {

                if (value == null || value.isBlank()) {

                        return List.of();
                }

                return Arrays.stream(value.split(","))

                                .map(String::trim)

                                .toList();
        }

        private List<CursorMetadataEntity> mapCursors(List<CursorMetadata> cursors) {

                if (cursors == null || cursors.isEmpty()) {

                        return List.of();
                }

                return cursors.stream()

                                .map(cursor -> {

                                        CursorMetadataEntity entity = new CursorMetadataEntity();

                                        entity.setCursorName(cursor.cursorName());

                                        entity.setBulkCollect(cursor.bulkCollect());

                                        entity.setForUpdate(cursor.forUpdate());

                                        entity.setForall(cursor.forall());

                                        return entity;
                                })

                                .collect(Collectors.toList());
        }

        private List<SubprogramNodeEntity> mapSubprograms(List<SubprogramNode> subprograms) {

                if (subprograms == null || subprograms.isEmpty()) {

                        return List.of();
                }

                return subprograms.stream()

                                .map(subprogram -> {

                                        SubprogramNodeEntity entity = new SubprogramNodeEntity();
                                        entity.setSubprogramName(subprogram.getName());

                                        entity.setSubprogramType(subprogram.getType());

                                        return entity;
                                })

                                .toList();
        }

private List<BusinessRuleEntity> mapBusinessRules(List<BusinessRuleMetadata> businessRules) {

    if (businessRules == null) {
        return List.of();
    }

    return businessRules.stream()

            .map(rule -> {

                BusinessRuleEntity entity =
                        new BusinessRuleEntity();

                entity.setErrorCode(rule.errorCode());

                entity.setMessage(rule.message());

                return entity;
            })

            .toList();
}

}
