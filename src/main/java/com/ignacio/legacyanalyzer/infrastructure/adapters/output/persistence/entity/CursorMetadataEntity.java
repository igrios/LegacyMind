package com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence.entity;

import com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence.LegacyObjectEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "cursor_metadata")
@Getter
@Setter
public class CursorMetadataEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "cursor_name")
    private String cursorName;

    @Column(name = "bulk_collect")
    private boolean bulkCollect;

    @Column(name = "for_update")
    private boolean forUpdate;

    @Column(name = "forall_usage")
    private boolean forall;

    @ManyToOne
    @JoinColumn(name = "legacy_object_id")
    private LegacyObjectEntity legacyObject;

    public LegacyObjectEntity getLegacyObject() {

        return legacyObject;
    }

    public void setLegacyObject(
            LegacyObjectEntity legacyObject) {

        this.legacyObject = legacyObject;
    }
}