package com.ignacio.legacyanalyzer.domain.services;

import com.ignacio.legacyanalyzer.domain.model.TableDependency;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DependencyAnalyzerService {

    public List<TableDependency> buildDependencies(List<String> tables, String objectName) {

        List<TableDependency> dependencies = new ArrayList<>();

        for (int i = 0; i < tables.size() - 1; i++) {

            dependencies.add(
                    new TableDependency(
                            tables.get(i),
                            tables.get(i + 1),
                            objectName
                    )
            );
        }

        return dependencies;
    }
}
