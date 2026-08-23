package com.ignacio.legacyanalyzer.domain.ports;

import java.util.List;
import com.ignacio.legacyanalyzer.domain.model.LegacyObject;
import com.ignacio.legacyanalyzer.domain.model.TableDependency;

/**
 * Output port that persists every artifact produced by one legacy analysis.
 */
public interface PersistAnalysisPort {

    void persist(LegacyObject legacyObject, List<TableDependency> dependencies);
}
