package com.ignacio.legacyanalyzer.infrastructure.adapters.output.parser;

import java.util.List;
import org.springframework.stereotype.Component;
import com.ignacio.legacyanalyzer.domain.model.CursorMetadata;
import com.ignacio.legacyanalyzer.domain.model.ExceptionMetadata;
import com.ignacio.legacyanalyzer.domain.services.semantic.CursorSemanticExtractor;
import com.ignacio.legacyanalyzer.domain.services.semantic.ExceptionSemanticExtractor;

@Component
public class CursorAndExceptionExtractor {

    private final CursorSemanticExtractor cursorExtractor;
    private final ExceptionSemanticExtractor exceptionExtractor;

    public CursorAndExceptionExtractor(
            CursorSemanticExtractor cursorExtractor,
            ExceptionSemanticExtractor exceptionExtractor) {
        this.cursorExtractor = cursorExtractor;
        this.exceptionExtractor = exceptionExtractor;
    }

    public Result extract(String normalizedSource) {
        return new Result(cursorExtractor.extractCursors(normalizedSource),
                exceptionExtractor.extract(normalizedSource));
    }

    public record Result(List<CursorMetadata> cursors, List<ExceptionMetadata> exceptions) {
    }
}
