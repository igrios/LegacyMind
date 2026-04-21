package com.ignacio.legacyanalyzer.domain.ports;

import com.ignacio.legacyanalyzer.domain.model.LegacyObject;

public interface LegacyParserPort {

    LegacyObject parse(String sourceCode);

}