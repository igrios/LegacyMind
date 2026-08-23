package com.ignacio.legacyanalyzer.domain.model;

public record KnowledgeRelation(

        String source,

        String relation,

        String target,

        String sourceObject,

        Integer sourceLineStart,

        Integer sourceLineEnd,

        String codeSnippet,

        Double confidenceLevel,

        String analysisId

) {

    public KnowledgeRelation(String source, String relation, String target) {
        this(source, relation, target, source, null, null, null, null, null);
    }
}
