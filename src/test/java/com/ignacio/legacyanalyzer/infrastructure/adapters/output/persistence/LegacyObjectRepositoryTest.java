package com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class LegacyObjectRepositoryTest {

    @Autowired
    private LegacyObjectRepository repository;

    @Test
    void shouldReturnLatestAnalysisWhenObjectNameIsDuplicated() {
        repository.saveAllAndFlush(List.of(
                entity("analysis-old", LocalDateTime.of(2026, 1, 1, 10, 0)),
                entity("analysis-latest", LocalDateTime.of(2026, 1, 2, 10, 0))));

        var result = repository.findFirstByNameOrderByCreatedAtDesc("PKG_DUPLICATED");

        assertTrue(result.isPresent());
        assertEquals("analysis-latest", result.orElseThrow().getId());
    }

    private LegacyObjectEntity entity(String id, LocalDateTime createdAt) {
        return new LegacyObjectEntity(
                id,
                "PKG_DUPLICATED",
                "PACKAGE",
                "BEGIN NULL; END;",
                "",
                "",
                "",
                0,
                "LOW",
                "Repository ordering test",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                createdAt);
    }
}
