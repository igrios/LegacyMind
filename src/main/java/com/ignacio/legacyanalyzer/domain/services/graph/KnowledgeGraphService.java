package com.ignacio.legacyanalyzer.domain.services.graph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import com.ignacio.legacyanalyzer.domain.model.GraphNode;
import com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence.KnowledgeRelationEntity;
import com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence.KnowledgeRelationRepository;
import com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence.LegacyObjectEntity;
import com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence.LegacyObjectRepository;
import com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence.SubprogramNodeRepository;
import com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence.entity.SubprogramNodeEntity;

@Service
public class KnowledgeGraphService {

    private final KnowledgeRelationRepository knowledgeRelationRepository;
    private final LegacyObjectRepository repository;
    private final SubprogramNodeRepository subprogramNodeRepository;

    public KnowledgeGraphService(KnowledgeRelationRepository knowledgeRelationRepository,
            LegacyObjectRepository repository, SubprogramNodeRepository subprogramNodeRepository    ) {

        this.knowledgeRelationRepository = knowledgeRelationRepository;
        this.repository = repository;
        this.subprogramNodeRepository = subprogramNodeRepository;
    }

    public Map<String, Object> buildGraph() {

        List<KnowledgeRelationEntity> relations = knowledgeRelationRepository.findAll();

        Map<String, String> nodeTypes = new HashMap<>();

        List<LegacyObjectEntity> legacyObjects = repository.findAll();

        List<SubprogramNodeEntity> subprograms = subprogramNodeRepository.findAll();

        legacyObjects.forEach(object ->

        nodeTypes.put(

                object.getName(),

                object.getType()));

   subprograms.forEach(subprogram ->

    nodeTypes.put(

            subprogram.getQualifiedName(),

            subprogram.getSubprogramType()));

                System.out.println(
        "SUBPROGRAMS SIZE >>> "
                + subprograms.size());

                subprograms.forEach(subprogram ->

        System.out.println(

                subprogram.getSubprogramName()

                        + " -> "

                        + subprogram.getSubprogramType()));

        Set<GraphNode> nodes = new HashSet<>();

        List<Map<String, String>> edges = relations.stream().map(relation -> {
            System.out.println(
        "SOURCE >>> "
                + relation.getSource());

System.out.println(
        "TARGET >>> "
                + relation.getTarget());

                
            nodes.add(          

                    new GraphNode(relation.getSource(),
                            nodeTypes.getOrDefault(relation.getSource(), "UNKNOWN")));

            nodes.add(new GraphNode(relation.getTarget(),
                    nodeTypes.getOrDefault(relation.getTarget(), "TABLE")));
            Map<String, String> edge = new HashMap<>();
            edge.put("source", relation.getSource());

            edge.put("target", relation.getTarget());

            edge.put("relation", relation.getRelation());

            return edge;

        }).toList();

        legacyObjects.forEach(object ->

        nodes.add(new GraphNode(object.getName(), object.getType())));

        Map<String, Object> result = new HashMap<>();

        result.put("nodes", nodes);

        result.put("edges", edges);

        return result;
    }
}
