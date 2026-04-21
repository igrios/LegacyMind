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

   private LocalDateTime createdAt;

    public LegacyObjectEntity() {
      }

   public LegacyObjectEntity(String id,
            String name,
            String type,
            String sourceCode,
            String procedures,
            String referencedTables,
            LocalDateTime createdAt){

              this.id = id;
        this.name = name;
        this.type = type;
        this.sourceCode = sourceCode;
        this.procedures = procedures;
        this.referencedTables = referencedTables;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }


}

