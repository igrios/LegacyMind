package com.ignacio.legacyanalyzer.infrastructure.adapters.output.parser;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ignacio.legacyanalyzer.domain.model.BusinessRuleMetadata;
import com.ignacio.legacyanalyzer.domain.model.DbLinkMetadata;
import com.ignacio.legacyanalyzer.domain.model.JoinCondition;
import com.ignacio.legacyanalyzer.domain.model.KnowledgeRelation;
import com.ignacio.legacyanalyzer.domain.model.LegacyObject;
import com.ignacio.legacyanalyzer.domain.model.RiskFinding;
import com.ignacio.legacyanalyzer.domain.model.SqlSemanticModel;
import com.ignacio.legacyanalyzer.domain.model.SubprogramNode;
import com.ignacio.legacyanalyzer.domain.model.TableReference;
import com.ignacio.legacyanalyzer.domain.ports.LegacyParserPort;
import com.ignacio.legacyanalyzer.domain.services.risk.LegacyRiskAnalyzer;
import com.ignacio.legacyanalyzer.domain.services.semantic.BusinessRuleExtractor;
import com.ignacio.legacyanalyzer.domain.services.semantic.CursorSemanticExtractor;
import com.ignacio.legacyanalyzer.domain.services.semantic.DbLinkSemanticExtractor;
import com.ignacio.legacyanalyzer.domain.services.semantic.ExceptionSemanticExtractor;
import com.ignacio.legacyanalyzer.domain.services.semantic.GraphRelationExtractor;
import com.ignacio.legacyanalyzer.domain.services.semantic.SqlSemanticExtractor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RegexLegacyParserAdapter implements LegacyParserPort {

    private final GraphRelationExtractor graphRelationExtractor;
    private final StructuralObjectExtractor structuralExtractor;
    private final SubprogramExtractor subprogramExtractor;
    private final CursorAndExceptionExtractor cursorAndExceptionExtractor;
    private final DbLinkExtractor dbLinkExtractor;
    private final BusinessRuleExtractor businessRuleExtractor;
    private final OracleJoinExtractor joinExtractor;
    private final SemanticDependencyExtractor dependencyExtractor;
    private final SqlSemanticModelExtractor semanticModelExtractor;

    @Autowired
    public RegexLegacyParserAdapter(
            GraphRelationExtractor graphRelationExtractor,
            StructuralObjectExtractor structuralExtractor,
            SubprogramExtractor subprogramExtractor,
            CursorAndExceptionExtractor cursorAndExceptionExtractor,
            DbLinkExtractor dbLinkExtractor,
            BusinessRuleExtractor businessRuleExtractor,
            OracleJoinExtractor joinExtractor,
            SemanticDependencyExtractor dependencyExtractor,
            SqlSemanticModelExtractor semanticModelExtractor) {
        this.graphRelationExtractor = graphRelationExtractor;
        this.structuralExtractor = structuralExtractor;
        this.subprogramExtractor = subprogramExtractor;
        this.cursorAndExceptionExtractor = cursorAndExceptionExtractor;
        this.dbLinkExtractor = dbLinkExtractor;
        this.businessRuleExtractor = businessRuleExtractor;
        this.joinExtractor = joinExtractor;
        this.dependencyExtractor = dependencyExtractor;
        this.semanticModelExtractor = semanticModelExtractor;
    }

    /** Compatibility constructor for focused parser tests and non-Spring clients. */
    public RegexLegacyParserAdapter(
            LegacyRiskAnalyzer riskAnalyzer,
            GraphRelationExtractor graphRelationExtractor,
            SqlSemanticExtractor semanticExtractor,
            CursorSemanticExtractor cursorSemanticExtractor) {
        OracleJoinExtractor oracleJoinExtractor = new OracleJoinExtractor();
        SemanticDependencyExtractor semanticDependencies =
                new SemanticDependencyExtractor(semanticExtractor, oracleJoinExtractor);
        this.graphRelationExtractor = graphRelationExtractor;
        this.structuralExtractor = new StructuralObjectExtractor();
        this.subprogramExtractor = new SubprogramExtractor(semanticExtractor);
        this.cursorAndExceptionExtractor = new CursorAndExceptionExtractor(
                cursorSemanticExtractor, new ExceptionSemanticExtractor());
        this.dbLinkExtractor = new DbLinkExtractor(new DbLinkSemanticExtractor());
        this.businessRuleExtractor = new BusinessRuleExtractor();
        this.joinExtractor = oracleJoinExtractor;
        this.dependencyExtractor = semanticDependencies;
        this.semanticModelExtractor = new SqlSemanticModelExtractor(
                semanticExtractor, semanticDependencies, oracleJoinExtractor, riskAnalyzer);
    }

    @Override
    public LegacyObject parse(String sourceCode) {
        if (sourceCode == null || sourceCode.isBlank()) {
            throw new IllegalArgumentException("Source code cannot be null or empty");
        }

        String normalized = dependencyExtractor.normalizeSourceCode(sourceCode);
        StructuralObjectExtractor.Structure structure = structuralExtractor.extract(sourceCode);
        List<SubprogramNode> subprograms =
                subprogramExtractor.extract(sourceCode, structure.name());

        SqlSemanticModel semanticModel = semanticModelExtractor.extract(normalized);

        String analysisId = UUID.randomUUID().toString();
        List<KnowledgeRelation> knowledgeRelations =
                graphRelationExtractor.extractKnowledgeRelations(
                        sourceCode, structure.name(), subprograms, analysisId);
        CursorAndExceptionExtractor.Result executionMetadata =
                cursorAndExceptionExtractor.extract(normalized);
        List<BusinessRuleMetadata> businessRules = businessRuleExtractor.extract(sourceCode);
        List<DbLinkMetadata> dbLinks = dbLinkExtractor.extract(sourceCode);
        List<String> referencedTables = Stream.concat(semanticModel.getReadTables().stream(),
                semanticModel.getWriteTables().stream()).distinct().toList();

        log.debug("Parsed {} {}: subprograms={}, tables={}, relations={}, cursors={}, exceptions={}, dbLinks={}",
                structure.type(), structure.name(), subprograms.size(), referencedTables.size(),
                knowledgeRelations.size(), executionMetadata.cursors().size(),
                executionMetadata.exceptions().size(), dbLinks.size());

        return new LegacyObject(
                analysisId, structure.name(), structure.type(), structure.procedures(),
                referencedTables, subprograms, executionMetadata.cursors(), businessRules,
                executionMetadata.exceptions(), dbLinks, knowledgeRelations, sourceCode,
                semanticModel.getFindings().stream().map(RiskFinding::toString).toList(),
                semanticModel.getRiskScore(), semanticModel.getRiskLevel(),
                buildSummary(structure.type(), structure.name(), referencedTables, semanticModel));
    }

    @Override
    public List<String> extractSemanticRelations(String sql) {
        return dependencyExtractor.extract(sql);
    }

    public String extractTopLevelFromClause(String sql) {
        return joinExtractor.extractTopLevelFromClause(sql);
    }

    public List<TableReference> extractTableReferences(String clause) {
        return joinExtractor.extractTableReferences(clause);
    }

    public List<JoinCondition> extractJoinConditions(String sql) {
        return joinExtractor.extractJoinConditions(sql);
    }

    public void resolveJoinConditions(
            List<TableReference> references, List<JoinCondition> conditions) {
        joinExtractor.resolveJoinConditions(references, conditions);
    }

    public SqlSemanticModel buildSemanticModel(String sql) {
        return semanticModelExtractor.extract(sql);
    }

    public boolean hasDeleteWithoutWhere(String sql) {
        return semanticModelExtractor.hasDeleteWithoutWhere(sql);
    }

    private String buildSummary(
            String type, String name, List<String> referencedTables, SqlSemanticModel model) {
        return String.format("The %s %s interacts with %d tables and has a risk level of %s.",
                type, name, referencedTables.size(), model.getRiskLevel());
    }

}
