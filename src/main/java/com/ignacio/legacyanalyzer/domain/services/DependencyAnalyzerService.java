package com.ignacio.legacyanalyzer.domain.services;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import com.ignacio.legacyanalyzer.domain.model.TableDependency;

@Service
public class DependencyAnalyzerService {

    public List<TableDependency> buildDependencies(List<String> tables, String objectName) {

        List<TableDependency> dependencies = new ArrayList<>();

        List<String> cleaned = tables.stream()
                .map(this::clean)
                .filter(s -> !s.isBlank())
                .distinct()
                .toList();

        // 🔥 GRAFO SECUENCIAL (clave)
        for (int i = 0; i < cleaned.size() - 1; i++) {

            String source = cleaned.get(i + 1);
            String target = cleaned.get(i);

            if (!source.equals(target)) {
                dependencies.add(new TableDependency(source, target, objectName));
            }
        }

        return dependencies;
    }

    private String clean(String table) {
        return table.replaceAll("[^a-zA-Z0-9_]", "").toUpperCase().trim();
    }
}