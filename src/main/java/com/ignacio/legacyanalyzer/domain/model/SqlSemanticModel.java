package com.ignacio.legacyanalyzer.domain.model;

import java.util.ArrayList;
import java.util.List;

public class SqlSemanticModel {

    private List<String> readTables = new ArrayList<>();

    private List<String> writeTables = new ArrayList<>();

    private List<TableReference> tableReferences = new ArrayList<>();

    private List<JoinCondition> joinConditions = new ArrayList<>();

    private List<String> semanticRelations = new ArrayList<>();

    public List<String> getReadTables() {
        return readTables;
    }

    public void setReadTables(List<String> readTables) {
        this.readTables = readTables;
    }

    public List<String> getWriteTables() {
        return writeTables;
    }

    public void setWriteTables(List<String> writeTables) {
        this.writeTables = writeTables;
    }

    public List<TableReference> getTableReferences() {
        return tableReferences;
    }

    public void setTableReferences(List<TableReference> tableReferences) {
        this.tableReferences = tableReferences;
    }

    public List<JoinCondition> getJoinConditions() {
        return joinConditions;
    }

    public void setJoinConditions(List<JoinCondition> joinConditions) {
        this.joinConditions = joinConditions;
    }

    public List<String> getSemanticRelations() {
        return semanticRelations;
    }

    public void setSemanticRelations(List<String> semanticRelations) {
        this.semanticRelations = semanticRelations;
    }

    @Override
    public String toString() {

        return """
                SqlSemanticModel{
                    readTables=%s,
                    writeTables=%s,
                    tableReferences=%s,
                    joinConditions=%s,
                    semanticRelations=%s
                }
                """.formatted(
                readTables,
                writeTables,
                tableReferences,
                joinConditions,
                semanticRelations);
    }
}