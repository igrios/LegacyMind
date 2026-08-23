package com.ignacio.legacyanalyzer.infrastructure.adapters.output.parser;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import com.ignacio.legacyanalyzer.domain.model.JoinCondition;
import com.ignacio.legacyanalyzer.domain.model.SqlSemanticModel;
import com.ignacio.legacyanalyzer.domain.model.TableReference;
import com.ignacio.legacyanalyzer.domain.services.risk.LegacyRiskAnalyzer;
import com.ignacio.legacyanalyzer.domain.services.semantic.SqlSemanticExtractor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SqlSemanticModelExtractor {

    private static final Pattern DELETE_PATTERN = Pattern.compile(
            "\\bDELETE\\s+FROM\\s+([A-Z][A-Z0-9_]*(?:\\.[A-Z][A-Z0-9_]*)?)",
            Pattern.CASE_INSENSITIVE);

    private final SqlSemanticExtractor semanticExtractor;
    private final SemanticDependencyExtractor dependencyExtractor;
    private final OracleJoinExtractor joinExtractor;
    private final LegacyRiskAnalyzer riskAnalyzer;

    public SqlSemanticModelExtractor(
            SqlSemanticExtractor semanticExtractor,
            SemanticDependencyExtractor dependencyExtractor,
            OracleJoinExtractor joinExtractor,
            LegacyRiskAnalyzer riskAnalyzer) {
        this.semanticExtractor = semanticExtractor;
        this.dependencyExtractor = dependencyExtractor;
        this.joinExtractor = joinExtractor;
        this.riskAnalyzer = riskAnalyzer;
    }

    public SqlSemanticModel extract(String sql) {
        SqlSemanticModel model = new SqlSemanticModel();
        String normalized = dependencyExtractor.normalize(sql);
        List<TableReference> references = joinExtractor.extractTableReferences(
                joinExtractor.extractTopLevelFromClause(normalized));
        List<JoinCondition> joins = joinExtractor.extractJoinConditions(normalized);

        model.setOriginalSql(sql);
        model.setReadTables(semanticExtractor.extractReadTables(normalized));
        model.setWriteTables(semanticExtractor.extractWriteTables(normalized));
        model.setSemanticRelations(dependencyExtractor.extract(normalized));
        model.setTableReferences(references);
        model.setJoinConditions(joins);
        riskAnalyzer.analyzeRisks(model);
        return model;
    }

    public boolean hasDeleteWithoutWhere(String sql) {
        Matcher matcher = DELETE_PATTERN.matcher(sql);
        while (matcher.find()) {
            int semicolon = sql.indexOf(";", matcher.start());
            String statement = semicolon == -1
                    ? sql.substring(matcher.start())
                    : sql.substring(matcher.start(), semicolon);
            if (!statement.contains(" WHERE ")) {
                log.debug("DELETE WITHOUT WHERE >>> {}", statement);
                return true;
            }
        }
        return false;
    }
}
