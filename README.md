# LegacyMind

## Overview

LegacyMind is a micro-SaaS built in Java using Hexagonal Architecture focused on analyzing, understanding, and modernizing legacy PL/SQL and Oracle systems.

The goal is not only to parse legacy code, but to discover:

- what each package / procedure / function really does
- which tables it impacts
- what dependencies exist
- what technical risks it contains
- what may break if it is modified
- how to reduce modernization risk

LegacyMind transforms tribal knowledge into persistent knowledge.

---

## Problem It Solves

In many large companies such as:

- Banks
- Telecom
- Insurance
- FinTech
- Utilities
- Public sector

there are legacy systems where:

> “Only one person knows how that package works.”

Typical example:

> “Don’t touch that package, it breaks billing.”

but nobody knows exactly why.

That is tribal knowledge.

LegacyMind converts that into persistent, searchable, and actionable information.

---

## Commercial Value

LegacyMind does NOT sell:

> a PL/SQL parser

LegacyMind sells:

# Legacy Modernization Risk Reduction

Companies do not buy regex.

They buy:

- reduced uncertainty
- lower production risk
- less dependency on key people
- better decision making
- safer modernization

---

## Tech Stack

### Backend

- Java 21
- Spring Boot 3.5.13
- Maven
- Spring Data JPA
- PostgreSQL
- Hexagonal Architecture

### Development

- Git
- GitHub
- VS Code
- IntelliJ IDEA
- Linux Mint

### Future AI Layer

- Ollama (local AI)
- pgvector
- semantic memory
- modernization suggestions
- explanation engine

---

## Project Structure

```text
src/main/java/com/ignacio/legacyanalyzer
│
├── application
│   ├── dto
│   └── usecase
│
├── domain
│   ├── model
│   ├── ports
│   └── services
│
├── infrastructure
│   ├── adapters
│   │   ├── input
│   │   │   └── controller
│   │   │
│   │   └── output
│   │       ├── parser
│   │       └── persistence
│   │
│   └── config
│
└── LegacyAnalyzerApplication.java
```

---

## Implemented Features

## 1. Legacy Object Parser

Detects:

- PACKAGE
- PROCEDURE
- FUNCTION

Extracts:

- objectName
- objectType
- internal procedures
- referencedTables

### Example

```sql
CREATE OR REPLACE PROCEDURE sync_customer AS
BEGIN
    SELECT *
    INTO v_customer
    FROM customer_table
    JOIN debt_table
        ON customer_table.id = debt_table.customer_id;

    UPDATE audit_table
    SET updated_at = SYSDATE;
END;
```

### Output

```json
{
  "name": "SYNC_CUSTOMER",
  "type": "PROCEDURE",
  "procedures": [
    "SYNC_CUSTOMER"
  ],
  "referencedTables": [
    "DEBT_TABLE",
    "CUSTOMER_TABLE",
    "AUDIT_TABLE",
    "V_CUSTOMER"
  ]
}
```

---

## 2. Code Smell Detection

Currently detects:

- SELECT *
- COMMIT inside procedure
- WHEN OTHERS generic exception handling
- Dynamic SQL (EXECUTE IMMEDIATE)

### Example

```sql
CREATE OR REPLACE PROCEDURE process_refund AS
BEGIN
    SELECT * FROM refund_table;
    COMMIT;
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
END;
```

### Result

```json
"codeSmells": [
  "SELECT * detected",
  "COMMIT inside procedure",
  "WHEN OTHERS generic exception handling"
]
```

---

## 3. Risk Score Engine

Each smell contributes to a technical risk score.

### Current scoring

- SELECT * → +2
- COMMIT → +2
- WHEN OTHERS → +3
- EXECUTE IMMEDIATE → +4

### Risk levels

- 0–2 → LOW
- 3–6 → MEDIUM
- 7+ → HIGH

### Example

```json
{
  "riskScore": 11,
  "riskLevel": "HIGH"
}
```

---

## 4. PostgreSQL Persistence

Stored data includes:

- name
- type
- procedures
- referencedTables
- codeSmells
- riskScore
- riskLevel
- sourceCode
- createdAt

This turns LegacyMind into a knowledge system, not just a parser.

---

## REST API

## POST Analyze Legacy Code

### Endpoint

```http
POST /api/legacy/analyze
```

### CURL Example

```bash
curl -X POST http://localhost:8080/api/legacy/analyze \
-H "Content-Type: application/json" \
-d '{
  "sourceCode": "CREATE OR REPLACE PROCEDURE generate_dynamic_report AS v_sql VARCHAR2(1000); BEGIN SELECT * FROM report_table; v_sql := '\''UPDATE audit_table SET updated_at = SYSDATE WHERE id = 1'\''; EXECUTE IMMEDIATE v_sql; COMMIT; EXCEPTION WHEN OTHERS THEN ROLLBACK; END;"
}'
```

### Response

```json
{
  "name": "GENERATE_DYNAMIC_REPORT",
  "type": "PROCEDURE",
  "procedures": [
    "GENERATE_DYNAMIC_REPORT"
  ],
  "referencedTables": [
    "REPORT_TABLE",
    "AUDIT_TABLE"
  ],
  "codeSmells": [
    "SELECT * detected",
    "COMMIT inside procedure",
    "WHEN OTHERS generic exception handling",
    "Dynamic SQL detected (EXECUTE IMMEDIATE)"
  ],
  "riskScore": 11,
  "riskLevel": "HIGH"
}
```

---

## GET Analysis History

### Endpoint

```http
GET /api/legacy/history
```

### CURL Example

```bash
curl http://localhost:8080/api/legacy/history
```

### Response

```json
[
  {
    "name": "VALIDATE_CREDIT_LIMIT",
    "type": "FUNCTION",
    "riskScore": 7,
    "riskLevel": "HIGH"
  },
  {
    "name": "GENERATE_DYNAMIC_REPORT",
    "type": "PROCEDURE",
    "riskScore": 11,
    "riskLevel": "HIGH"
  }
]
```

---

## Technical Debt Identified

## Oracle Legacy Implicit Joins

Classic case:

```sql
SELECT *
FROM customer_table c,
     debt_table d,
     audit_table a
WHERE c.id = d.customer_id
AND d.id = a.debt_id(+)
```

Currently the parser handles:

```sql
explicit JOIN
```

better than:

```sql
table_a, table_b
```

This is registered as prioritized technical debt.

Because the real value is in old syntax.

Not modern SQL.

---

## Difference vs SonarQube

SonarQube asks:

> Is this code well written?

LegacyMind asks:

> What does this legacy object really do and what happens if I modify it?

That is the real differentiation.

Not competing as a static analysis tool.

But as a:

# Legacy Modernization Intelligence Platform

---

## Next Steps

### Short Term

- functionalSummary
- better parser precision
- implicit Oracle joins support
- more code smells
- dependency mapping

### Mid Term

- automatic technical documentation
- business impact explanation
- modernization suggestions

### Long Term

- Ollama integration
- pgvector semantic memory
- AI-assisted modernization strategy

---

## Final Statement

LegacyMind does not analyze only code.

It helps companies survive legacy systems.

Because:

> Modern software is developed

> Legacy software is survived
