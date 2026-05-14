package com.ignacio.legacyanalyzer.domain.model;

public class JoinCondition {

   private final String leftAlias;

    private final String leftColumn;

    private final String rightAlias;

    private final String rightColumn;

public JoinCondition(String leftAlias, String leftColumn, String rightAlias, String rightColumn) {
        this.leftAlias = leftAlias;
        this.leftColumn = leftColumn;
        this.rightAlias = rightAlias;
        this.rightColumn = rightColumn;
    }

    public String getLeftAlias() {
        return leftAlias;
    }

    public String getLeftColumn() {
        return leftColumn;
    }

    public String getRightAlias() {
        return rightAlias;
    }

    public String getRightColumn() {
        return rightColumn;
    }

@Override
    public String toString() {
        return leftAlias + "." + leftColumn
                + " -> "
                + rightAlias + "." + rightColumn;
    }

}
