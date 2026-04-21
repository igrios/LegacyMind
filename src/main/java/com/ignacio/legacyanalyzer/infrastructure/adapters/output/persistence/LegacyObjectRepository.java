package com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LegacyObjectRepository extends JpaRepository<LegacyObjectEntity, String>     {

}
