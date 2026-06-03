package com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence.entity;

import com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence.LegacyObjectEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "dblink_metadata")
@Getter
@Setter
public class DbLinkMetadataEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "db_link_name")
    private String dbLinkName;

    @Column(name = "remote_object")
    private String remoteObject;

    @ManyToOne
    @JoinColumn(name = "legacy_object_id")
    private LegacyObjectEntity legacyObject;
}