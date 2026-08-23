package com.ignacio.legacyanalyzer.domain.ports;

import java.util.List;
import com.ignacio.legacyanalyzer.domain.model.LegacyObject;

public interface LegacyParserPort {

    LegacyObject parse(String sourceCode);

    List<String> extractSemanticRelations(String sourceCode);

}
