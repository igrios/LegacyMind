package com.ignacio.legacyanalyzer.infrastructure.adapters.output.parser;

import java.util.List;
import org.springframework.stereotype.Component;
import com.ignacio.legacyanalyzer.domain.model.DbLinkMetadata;
import com.ignacio.legacyanalyzer.domain.services.semantic.DbLinkSemanticExtractor;

@Component
public class DbLinkExtractor {

    private final DbLinkSemanticExtractor delegate;

    public DbLinkExtractor(DbLinkSemanticExtractor delegate) {
        this.delegate = delegate;
    }

    public List<DbLinkMetadata> extract(String sourceCode) {
        return delegate.extract(sourceCode);
    }
}
