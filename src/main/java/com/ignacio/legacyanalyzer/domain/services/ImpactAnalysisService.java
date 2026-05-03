package com.ignacio.legacyanalyzer.domain.services;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import org.springframework.stereotype.Service;
import com.ignacio.legacyanalyzer.domain.ports.TableDependencyRepositoryPort;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Service
public class ImpactAnalysisService {

    private final TableDependencyRepositoryPort repository;
    private final TableDependencyRepositoryPort dependencyRepositoryPort;

    public ImpactAnalysisService(TableDependencyRepositoryPort repository, TableDependencyRepositoryPort dependencyRepositoryPort) {
        this.repository = repository;
        this.dependencyRepositoryPort = dependencyRepositoryPort;
    }

    public Set<String> calculateCascadeImpact(String table) {
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();

        queue.add(table);

        while (!queue.isEmpty()) {
            String current = queue.poll();

            if (!visited.contains(current)) {
                visited.add(current);

                List<String> dependents = repository.findTargetsBySource(current);
                queue.addAll(dependents);
            }
        }

        return visited;
    }

public Set<String> getImpact(String startTable) {

    Set<String> visited = new LinkedHashSet<>();
    Queue<String> queue = new LinkedList<>();

    queue.add(startTable);
    visited.add(startTable);

    while (!queue.isEmpty()) {
        String current = queue.poll();

        List<String> neighbors = dependencyRepositoryPort.findTargetsBySource(current);

        for (String neighbor : neighbors) {
            if (!visited.contains(neighbor)) {
                visited.add(neighbor);
                queue.add(neighbor);
            }
        }
    }

    return visited;
}
public Map<Integer, Set<String>> getImpactByLevels(String startTable) {

    Map<Integer, Set<String>> levels = new LinkedHashMap<>();
    Set<String> visited = new HashSet<>();
    Queue<String> queue = new LinkedList<>();

    queue.add(startTable);
    visited.add(startTable);

    int level = 0;

    while (!queue.isEmpty()) {
        int size = queue.size();
        Set<String> currentLevel = new LinkedHashSet<>();

        for (int i = 0; i < size; i++) {
            String current = queue.poll();
            currentLevel.add(current);

            List<String> neighbors =
                    dependencyRepositoryPort.findTargetsBySource(current);

            for (String neighbor : neighbors) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        levels.put(level, currentLevel);
        level++;
    }

    return levels;
}
public List<List<String>> getAllPaths(String startTable) {

    List<List<String>> paths = new ArrayList<>();
    LinkedList<String> currentPath = new LinkedList<>();

    dfs(startTable, currentPath, paths);

    return paths;
}
private void dfs(String current,
                 LinkedList<String> path,
                 List<List<String>> paths) {

    path.add(current);

    List<String> neighbors =
            dependencyRepositoryPort.findTargetsBySource(current);

    if (neighbors.isEmpty()) {
        paths.add(new ArrayList<>(path));
    } else {
        for (String next : neighbors) {
            if (!path.contains(next)) {
                dfs(next, path, paths);
            }
        }
    }

    path.removeLast();
}

    
}