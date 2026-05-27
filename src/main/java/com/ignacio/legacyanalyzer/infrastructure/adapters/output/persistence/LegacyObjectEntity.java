package com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence;

import java.time.LocalDateTime;
import java.util.List;
import com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence.entity.CursorMetadataEntity;
import com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence.entity.SubprogramNodeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "legacy_objects")

public class LegacyObjectEntity {

  @Id
  private String id;

  private String name;
  
  private String type;
  @Column(columnDefinition = "TEXT")
  private String sourceCode;

   @Column(columnDefinition = "TEXT")
   private String procedures;

   @Column(columnDefinition = "TEXT")
   private String referencedTables;
   
   private String codeSmells;

   private Integer riskScore;

   private String riskLevel;

   private LocalDateTime createdAt;



@Column(name = "functional_summary")
private String functionalSummary;


@OneToMany(
        cascade = CascadeType.ALL,
        orphanRemoval = true)
@JoinColumn(name = "legacy_object_id")
private List<CursorMetadataEntity> cursors;

@OneToMany(
        cascade = CascadeType.ALL,
        orphanRemoval = true)
@JoinColumn(name = "legacy_object_id")
private List<SubprogramNodeEntity> subprograms;




    public LegacyObjectEntity() {
      }

   public LegacyObjectEntity(String id,
            String name,
            String type,
            String sourceCode,
            String procedures,
            String referencedTables,
            String codeSmells,
            Integer riskScore,
            String riskLevel,
            String functionalSummary,
            List<CursorMetadataEntity> cursors,
            List<SubprogramNodeEntity> subprograms,
            LocalDateTime createdAt  ){

              this.id = id;
        this.name = name;
        this.type = type;
        this.sourceCode = sourceCode;
        this.procedures = procedures;
        this.referencedTables = referencedTables;
        this.codeSmells = codeSmells;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.createdAt = createdAt;
        this.functionalSummary = functionalSummary;
        this.cursors = cursors;
        this.subprograms = subprograms;
   }

public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public String getProcedures() {
        return procedures;
    }

    public String getReferencedTables() {
        return referencedTables;
    }
    public String getCodeSmells() {
    return codeSmells;
}

   public Integer getRiskScore() {
    return riskScore;
     }

    public String getRiskLevel() {
    return riskLevel;
  }

public String getFunctionalSummary() {
    return functionalSummary;
}

public List<CursorMetadataEntity> getCursors() {
    return cursors;
}   
public void setCursors(List<CursorMetadataEntity> cursors) {
    this.cursors = cursors; 
}

public List<SubprogramNodeEntity> getSubprograms() {

    return subprograms;
}

public void setSubprograms(
        List<SubprogramNodeEntity> subprograms) {

    this.subprograms = subprograms;
}

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }


}

