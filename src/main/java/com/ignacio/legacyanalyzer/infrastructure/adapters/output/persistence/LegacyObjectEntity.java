package com.ignacio.legacyanalyzer.infrastructure.adapters.output.persistence;

import java.time.LocalDateTime;
import jakarta.persistence.*;



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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }


}

