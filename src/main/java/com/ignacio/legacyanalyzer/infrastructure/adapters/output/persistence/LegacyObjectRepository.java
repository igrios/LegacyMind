package com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LegacyObjectRepository extends JpaRepository<LegacyObjectEntity, String> {

  Optional<LegacyObjectEntity> findFirstByNameOrderByCreatedAtDesc(String name);
}
