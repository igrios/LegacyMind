package com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence.entity;

import com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence.LegacyObjectEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "exception_metadata")
@Getter
@Setter       
public class ExceptionMetadataEntity {

  @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "exception_name")
    private String exceptionName;

    @Column(name = "generic_handler")
    private boolean genericHandler;

    @ManyToOne
    @JoinColumn(name = "legacy_object_id")
    private LegacyObjectEntity legacyObject;



}
