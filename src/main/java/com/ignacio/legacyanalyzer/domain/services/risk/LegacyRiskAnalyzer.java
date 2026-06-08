package com.ignacio.legacyanalyzer.domain.services;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import com.ignacio.legacyanalyzer.domain.model.RiskFinding;
import com.ignacio.legacyanalyzer.domain.model.SqlSemanticModel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class LegacyRiskAnalyzer {

        private static final Pattern SELECT_STAR_PATTERN =
                        Pattern.compile("\\bSELECT\\s+\\*", Pattern.CASE_INSENSITIVE);

        private static final Pattern COMMIT_PATTERN =
                        Pattern.compile("\\bCOMMIT\\b", Pattern.CASE_INSENSITIVE);

        private static final Pattern WHEN_OTHERS_PATTERN =
                        Pattern.compile("\\bWHEN\\s+OTHERS\\b", Pattern.CASE_INSENSITIVE);


        private static final Pattern EXECUTE_IMMEDIATE_PATTERN =
                        Pattern.compile("\\bEXECUTE\\s+IMMEDIATE\\b", Pattern.CASE_INSENSITIVE);


        public void analyzeRisks(SqlSemanticModel model) {

                List<RiskFinding> findings = new ArrayList<>();

                // =============================
                // COMMIT
                // =============================



                if (hasCommit(model.getOriginalSql())) {

                        findings.add(new RiskFinding("MEDIUM", "COMMIT_USAGE",
                                        "COMMIT detected inside procedure"));
                }

                // =============================
                // WHEN OTHERS
                // =============================

                if (hasWhenOthers(model.getOriginalSql())) {

                        findings.add(new RiskFinding("HIGH", "GENERIC_EXCEPTION",
                                        "WHEN OTHERS generic exception handling detected"));
                }

                // =============================
                // EXECUTE IMMEDIATE
                // =============================

                if (hasExecuteImmediate(model.getOriginalSql())) {

                        findings.add(

                                        new RiskFinding(

                                                        "HIGH",

                                                        "DYNAMIC_SQL",

                                                        "Dynamic SQL detected using EXECUTE IMMEDIATE"));
                }


                // =============================
                // CARTESIAN JOIN
                // =============================

                if (hasCartesianJoin(model)) {

                        findings.add(new RiskFinding("HIGH", "CARTESIAN_JOIN",
                                        "Possible cartesian join detected"));
                }

                // =============================
                // SELECT *
                // =============================

                if (hasSelectStar(model.getOriginalSql())) {

                        findings.add(new RiskFinding("MEDIUM", "SELECT_STAR",
                                        "Avoid using SELECT *"));
                }

                model.setFindings(findings);

                calculateRisk(model);

                log.debug("RISK FINDINGS >>> {}", findings);

                log.debug("RISK SCORE >>> {}", model.getRiskScore());

                log.debug("RISK LEVEL >>> {}", model.getRiskLevel());
        }

        private void calculateRisk(SqlSemanticModel model) {

                int score = 0;

                for (RiskFinding finding : model.getFindings()) {

                        switch (finding.getSeverity()) {

                                case "LOW" -> score += 1;

                                case "MEDIUM" -> score += 3;

                                case "HIGH" -> score += 7;

                                case "CRITICAL" -> score += 15;
                        }
                }

                model.setRiskScore(score);

                if (score >= 15) {

                        model.setRiskLevel("CRITICAL");

                } else if (score >= 7) {

                        model.setRiskLevel("HIGH");

                } else if (score >= 3) {

                        model.setRiskLevel("MEDIUM");

                } else {

                        model.setRiskLevel("LOW");
                }
        }

        public boolean hasCartesianJoin(SqlSemanticModel model) {

                int tableCount = model.getTableReferences().size();

                int joinCount = model.getJoinConditions().size();

                boolean cartesian = tableCount > 1 && joinCount == 0;

                log.debug("CARTESIAN CHECK >>> tables={} joins={} result={}", tableCount, joinCount,
                                cartesian);

                return cartesian;
        }

        public boolean hasSelectStar(String sql) {

                boolean result = SELECT_STAR_PATTERN.matcher(sql).find();

                log.debug("SELECT STAR CHECK >>> {}", result);

                return result;
        }

        public boolean hasCommit(String sql) {

                boolean result = COMMIT_PATTERN.matcher(sql).find();

                log.debug("COMMIT CHECK >>> {}", result);

                return result;
        }

        public boolean hasWhenOthers(String sql) {

                boolean result = WHEN_OTHERS_PATTERN.matcher(sql).find();

                log.debug("WHEN OTHERS CHECK >>> {}", result);

                return result;
        }

        public boolean hasExecuteImmediate(String sql) {

                boolean result = EXECUTE_IMMEDIATE_PATTERN.matcher(sql).find();

                log.debug("EXECUTE IMMEDIATE CHECK >>> {}", result);

                return result;
        }



}
