package com.ignacio.legacyanalyzer.infrastructure.adapters.output.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import com.ignacio.legacyanalyzer.domain.model.LegacyObject;
import com.ignacio.legacyanalyzer.domain.ports.LegacyParserPort;

@Component
public class RegexLegacyParserAdapter implements LegacyParserPort {

    @Override
    public LegacyObject parse(String sourceCode) {

        String normalized = sourceCode.toUpperCase();
String sqlWithoutFunctions = normalized.replaceAll("\\b[A-Z_]+\\s*\\([^()]*\\)", " ");
        // =============================
        // NAME + TYPE
        // =============================
        String name = null;
        String type = null;

        Pattern namePattern = Pattern.compile(
                "CREATE\\s+OR\\s+REPLACE\\s+(PACKAGE|PROCEDURE|FUNCTION)\\s+(BODY\\s+)?(\\w+)",
                Pattern.CASE_INSENSITIVE);

        Matcher nameMatcher = namePattern.matcher(sourceCode);

        if (nameMatcher.find()) {
            type = nameMatcher.group(1).toUpperCase();
            name = nameMatcher.group(3).toUpperCase();
        }

        // =============================
        // PROCEDURES
        // =============================
        Pattern procPattern =
                Pattern.compile("\\bPROCEDURE\\s+(\\w+)\\s*(\\(|IS|AS)", Pattern.CASE_INSENSITIVE);

        Matcher procMatcher = procPattern.matcher(sourceCode);

        List<String> procedures = new ArrayList<>();

        while (procMatcher.find()) {
            String procName = procMatcher.group(1).toUpperCase();

            if (name == null || !procName.equalsIgnoreCase(name)) {
                procedures.add(procName);
            }
        }

        procedures = procedures.stream().distinct().toList();

        // =============================
        // TABLES + ALIAS
        // =============================
        Map<String, String> aliasMap = extractTablesWithAlias(sourceCode);
        Set<String> tables = new HashSet<>(aliasMap.values());

        // =============================
        // JOIN / FROM EXTRACTION (SAFE)
        // =============================
        Pattern fromPattern =
                Pattern.compile("\\bFROM\\s+([A-Z0-9_\\.]+)", Pattern.CASE_INSENSITIVE);
        Matcher fromMatcher = fromPattern.matcher(sqlWithoutFunctions);

        while (fromMatcher.find()) {
            String table = fromMatcher.group(1);

            table = table.contains(".") ? table.split("\\.")[1] : table;

            if (isValidTable(table)) {
                tables.add(table);
            }
        }

//=============================
// 🔥 SOPORTE JOIN IMPLÍCITO (,)
// =============================
Pattern commaPattern = Pattern.compile(",\\s*([A-Z0-9_\\.]+)", Pattern.CASE_INSENSITIVE);
Matcher commaMatcher = commaPattern.matcher(sqlWithoutFunctions);

while (commaMatcher.find()) {

    String table = commaMatcher.group(1);

    table = table.contains(".") ? table.split("\\.")[1] : table;

    if (isValidTable(table)) {
        tables.add(table);
    }
}



        // =============================
        // UPDATE
        // =============================
        Pattern updatePattern = Pattern.compile("UPDATE\\s+(\\w+)", Pattern.CASE_INSENSITIVE);
        Matcher updateMatcher = updatePattern.matcher(sourceCode);

        while (updateMatcher.find()) {
            String table = clean(updateMatcher.group(1));
            if (isLikelyTable(table)) {
                tables.add(table);
            }
        }

        // =============================
        // INSERT
        // =============================
        Pattern insertPattern =
                Pattern.compile("INSERT\\s+INTO\\s+(\\w+)", Pattern.CASE_INSENSITIVE);
        Matcher insertMatcher = insertPattern.matcher(sourceCode);

        while (insertMatcher.find()) {
            String table = clean(insertMatcher.group(1));
            if (isLikelyTable(table)) {
                tables.add(table);
            }
        }

        List<String> referencedTables = tables.stream().filter(Objects::nonNull)
                .filter(s -> !s.isBlank()).distinct().toList();

        // =============================
        // RELACIONES SEMÁNTICAS
        // =============================
        List<String> relations = extractSemanticRelations(sourceCode);

        // =============================
        // CODE SMELLS
        // =============================
        List<String> smells = new ArrayList<>();

        if (normalized.contains("SELECT *"))
            smells.add("SELECT * detected");
        if (normalized.contains("COMMIT"))
            smells.add("COMMIT inside procedure");
        if (normalized.contains("WHEN OTHERS"))
            smells.add("WHEN OTHERS generic exception handling");
        if (normalized.contains("EXECUTE IMMEDIATE"))
            smells.add("Dynamic SQL detected");

        smells = smells.stream().distinct().toList();

        // =============================
        // RISK SCORE
        // =============================
        int score = 0;

        for (String smell : smells) {
            if (smell.contains("SELECT *"))
                score += 2;
            if (smell.contains("COMMIT"))
                score += 2;
            if (smell.contains("WHEN OTHERS"))
                score += 3;
            if (smell.contains("EXECUTE IMMEDIATE"))
                score += 4;
        }

        String riskLevel = score <= 2 ? "LOW" : score <= 6 ? "MEDIUM" : "HIGH";

        // =============================
        // SUMMARY
        // =============================
        String summary = "The " + type + " " + name + " interacts with " + referencedTables.size()
                + " tables and has a risk level of " + riskLevel + ".";

        return new LegacyObject(UUID.randomUUID().toString(), name, type, procedures,
                referencedTables, sourceCode, smells, score, riskLevel, summary);
    }

    // ======================================================
    // 🔥 ALIAS SAFE (FIX CLAVE)
    // ======================================================
    private Map<String, String> extractTablesWithAlias(String sql) {

        Map<String, String> aliasToTable = new HashMap<>();

        Pattern fromPattern = Pattern.compile("FROM\\s+(.*?)\\s+(WHERE|GROUP BY|ORDER BY|$)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

        Matcher matcher = fromPattern.matcher(sql);

        if (matcher.find()) {

            String[] tables = matcher.group(1).split(",");

            for (String table : tables) {

                String cleaned = table.trim().replaceAll("\\s+", " ");

                if (cleaned.isBlank())
                    continue;

                String[] parts = cleaned.split(" ");

                String tableName = clean(parts[0]);

                // 🔥 FILTRO CRÍTICO
                if (!isLikelyTable(tableName))
                    continue;

                if (parts.length == 1) {
                    aliasToTable.put(tableName, tableName);
                } else {
                    aliasToTable.put(parts[1].toUpperCase(), tableName);
                }
            }
        }

        return aliasToTable;
    }

    // ======================================================
    // 🔥 RELACIONES SEMÁNTICAS (ROBUSTO)
    // ======================================================
  // ======================================================
// 🔥 RELACIONES SEMÁNTICAS (ROBUSTO)
// ======================================================
public List<String> extractSemanticRelations(String sql) {

    List<String> relations = new ArrayList<>();

    // 🔹 Normalizar
    String normalized = sql.toUpperCase();

    // 🔥 Limpiar funciones (EXTRACT, COUNT, etc.)
    String sqlWithoutFunctions = normalized.replaceAll(
        "\\b[A-Z_]+\\s*\\([^()]*\\)", " "
    );

    // 🔹 Separar statements
    String[] statements = sqlWithoutFunctions.split(";");

    Pattern insertPattern = Pattern.compile("INSERT\\s+INTO\\s+(\\w+)");
    Pattern updatePattern = Pattern.compile("UPDATE\\s+(\\w+)");

    Pattern fromPattern = Pattern.compile(
        "\\bFROM\\s+([^\\(]*?)(WHERE|GROUP BY|ORDER BY|$)",
        Pattern.CASE_INSENSITIVE
    );

    for (String stmt : statements) {

        // =============================
        // INSERT INTO ... SELECT ...
        // =============================
        Matcher insertMatcher = insertPattern.matcher(stmt);
        Matcher fromMatcher = fromPattern.matcher(stmt);

        if (insertMatcher.find() && fromMatcher.find()) {

            String target = clean(insertMatcher.group(1));

            Set<String> sources = extractTables(fromMatcher.group(1));

            for (String source : sources) {
                relations.add(source + "->" + target);
            }
        }

        // =============================
        // UPDATE ... FROM ...
        // =============================
        Matcher updateMatcher = updatePattern.matcher(stmt);

        if (updateMatcher.find()) {

            String target = clean(updateMatcher.group(1));

            Matcher subFromMatcher = fromPattern.matcher(stmt);

            if (subFromMatcher.find()) {

                Set<String> sources = extractTables(subFromMatcher.group(1));

                for (String source : sources) {
                    relations.add(source + "->" + target);
                }
            }
        }
    }

    return relations;
}




 private Set<String> extractTables(String fromClause) {

    Set<String> tables = new HashSet<>();

    String[] parts = fromClause.split(",");

    for (String part : parts) {

        String cleaned = part.trim().replaceAll("\\s+", " ");

        if (cleaned.isBlank())
            continue;

        String table = cleaned.split(" ")[0];

        if (table.contains(".")) {
            table = table.split("\\.")[1];
        }

        table = clean(table);

        if (isValidTable(table)) {
            tables.add(table);
        }
    }

    return tables;
}

    private String clean(String table) {
        return table.replaceAll("[^A-Z0-9_]", "");
    }

    private boolean isLikelyTable(String name) {
        return name != null && name.matches("[A-Z_][A-Z0-9_]*") && !name.contains("SYSDATE")
                && !name.contains("DUAL") && !name.contains("COUNT") && !name.contains("SUM")
                && !name.contains("EXTRACT") && !name.contains("MONTH") && !name.contains("YEAR");
    }
private boolean isValidTable(String table) {
    return table != null
        && table.matches("[A-Z][A-Z0-9_]*")
        && !table.startsWith("P_")
        && !table.startsWith("V_")
        && !table.equals("SYSDATE")
        && !table.equals("DESC")         // 🔥 nuevo
        && !table.equals("COUNT")
        && !table.equals("SUM")
        && table.length() > 4;           // 🔥 clave
}
}