package com.ignacio.legacyanalyzer.application.usecase;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.ignacio.legacyanalyzer.domain.model.GraphNode;
import com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence.KnowledgeRelationEntity;
import com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence.KnowledgeRelationRepository;
import com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence.LegacyObjectEntity;
import com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence.LegacyObjectRepository;

@Component
public class GetKnowledgeGraphUseCase {

    private final KnowledgeRelationRepository knowledgeRelationRepository;

    private final LegacyObjectRepository repository;

    public GetKnowledgeGraphUseCase(
            KnowledgeRelationRepository knowledgeRelationRepository,
            LegacyObjectRepository repository) {

        this.knowledgeRelationRepository = knowledgeRelationRepository;
        this.repository = repository;
    }

    public Map<String, Object> execute() {

        List<KnowledgeRelationEntity> relations =
                knowledgeRelationRepository.findAll();

        Map<String, String> nodeTypes =
                new HashMap<>();

        List<LegacyObjectEntity> legacyObjects =
                repository.findAll();

        legacyObjects.forEach(object ->

                nodeTypes.put(

                        object.getName(),

                        object.getType()));

        Set<GraphNode> nodes =
                new HashSet<>();

        List<Map<String, String>> edges =
                relations.stream()

                        .map(relation -> {

                            nodes.add(

                                    new GraphNode(

                                            relation.getSource(),

                                            nodeTypes.getOrDefault(

                                                    relation.getSource(),

                                                    "UNKNOWN")));

                            nodes.add(

                                    new GraphNode(

                                            relation.getTarget(),

                                            nodeTypes.getOrDefault(

                                                    relation.getTarget(),

                                                    "TABLE")));

                            Map<String, String> edge =
                                    new HashMap<>();

                            edge.put(
                                    "source",
                                    relation.getSource());

                            edge.put(
                                    "target",
                                    relation.getTarget());

                            edge.put(
                                    "relation",
                                    relation.getRelation());

                            return edge;

                        })

                        .toList();

        legacyObjects.forEach(object ->

                nodes.add(

                        new GraphNode(

                                object.getName(),

                                object.getType())));

        Map<String, Object> result =
                new HashMap<>();

        result.put("nodes", nodes);

        result.put("edges", edges);

        return result;
    }
}