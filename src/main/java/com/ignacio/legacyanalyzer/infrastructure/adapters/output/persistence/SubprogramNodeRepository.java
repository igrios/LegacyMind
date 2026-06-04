package com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence.entity.SubprogramNodeEntity;

public interface SubprogramNodeRepository extends JpaRepository<SubprogramNodeEntity, String> {

}
