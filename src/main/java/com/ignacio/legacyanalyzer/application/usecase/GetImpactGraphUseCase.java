package com.ignacio.legacyanalyzer.application.usecase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import com.ignacio.legacyanalyzer.domain.services.graph.ImpactAnalysisService;

@Service
public class GetImpactGraphUseCase {

  private final ImpactAnalysisService impactService;

  public GetImpactGraphUseCase(ImpactAnalysisService impactService) {
    this.impactService = impactService;
  }

  public Map<String, Object> execute(String table) {

    List<List<String>> paths = impactService.getAllPaths(table);

    Set<String> nodes = new LinkedHashSet<>();
    Set<String> edgeKeys = new HashSet<>();
    List<Map<String, String>> edges = new ArrayList<>();

    for (List<String> path : paths) {

      for (int i = 0; i < path.size(); i++) {

        nodes.add(path.get(i));

        if (i < path.size() - 1) {

          String from = path.get(i);
          String to = path.get(i + 1);

          String key = from + "->" + to;

          if (!edgeKeys.contains(key)) {

            Map<String, String> edge = new HashMap<>();
            edge.put("from", from);
            edge.put("to", to);

            edges.add(edge);
            edgeKeys.add(key);
          }
        }
      }
    }

    Map<String, Object> result = new HashMap<>();
    result.put("nodes", nodes);
    result.put("edges", edges);

    return result;
  }
}
