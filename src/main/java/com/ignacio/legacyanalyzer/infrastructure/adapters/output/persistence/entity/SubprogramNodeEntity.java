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

@Entity
@Table(name = "subprogram_nodes")
public class SubprogramNodeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "subprogram_name")
    private String subprogramName;

    @Column(name = "qualified_name")
private String qualifiedName;

    @Column(name = "subprogram_type")
    private String subprogramType;

    @ManyToOne
    @JoinColumn(name = "legacy_object_id")
    private LegacyObjectEntity legacyObject;

    public String getId() {
        return id;
    }

    public String getSubprogramName() {
        return subprogramName;
    }

    public void setSubprogramName(
            String subprogramName) {

        this.subprogramName = subprogramName;
    }

    public String getSubprogramType() {
        return subprogramType;
    }

    public void setSubprogramType(
            String subprogramType) {

        this.subprogramType = subprogramType;
    }

    public LegacyObjectEntity getLegacyObject() {
        return legacyObject;
    }

    public void setLegacyObject(
            LegacyObjectEntity legacyObject) {

        this.legacyObject = legacyObject;
    }

public String getQualifiedName() {
        return qualifiedName;
    }

    public void setQualifiedName(String qualifiedName) {
        this.qualifiedName = qualifiedName;
    }   

    

}