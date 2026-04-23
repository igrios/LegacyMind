# LegacyMind

## Descripción General

LegacyMind es un micro-SaaS construido en Java con Arquitectura Hexagonal enfocado en analizar, entender y modernizar sistemas legacy en PL/SQL y Oracle.

El objetivo no es solamente parsear código legacy, sino descubrir:

- qué hace realmente cada package / procedure / function
- qué tablas impacta
- qué dependencias existen
- qué riesgos técnicos presenta
- qué puede romperse si se modifica
- cómo reducir el riesgo de modernización

LegacyMind transforma conocimiento tribal en conocimiento persistente.

---

## Problema que resuelve

En muchas empresas grandes como:

- Bancos
- Telecom
- Seguros
- Fintech
- Utilities
- Sector público

existen sistemas legacy donde:

> “Solo una persona sabe cómo funciona ese package.”

Ejemplo típico:

> “No toques ese package porque rompe facturación.”

pero nadie sabe exactamente por qué.

Eso es conocimiento tribal.

LegacyMind convierte eso en información persistente, consultable y accionable.

---

## Valor Comercial

LegacyMind NO vende:

> un parser de PL/SQL

LegacyMind vende:

# Reducción de riesgo en modernización legacy

Las empresas no compran regex.

Compran:

- reducción de incertidumbre
- menor riesgo en producción
- menor dependencia de personas clave
- mejor toma de decisiones
- modernización más segura

---

## Stack Tecnológico

### Backend

- Java 21
- Spring Boot 3.5.13
- Maven
- Spring Data JPA
- PostgreSQL
- Arquitectura Hexagonal

### Desarrollo

- Git
- GitHub
- VS Code
- IntelliJ IDEA
- Linux Mint

### Futuro Capa IA

- Ollama (IA local)
- pgvector
- memoria semántica
- sugerencias de modernización
- explanation engine

---

## Estructura del Proyecto

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

## Funcionalidades Implementadas

## 1. Parser de Objetos Legacy

Detecta:

- PACKAGE
- PROCEDURE
- FUNCTION

Extrae:

- objectName
- objectType
- procedures internas
- referencedTables

### Ejemplo

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

### Salida

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

## 2. Detección de Code Smells

Actualmente detecta:

- SELECT *
- COMMIT inside procedure
- WHEN OTHERS generic exception handling
- Dynamic SQL (EXECUTE IMMEDIATE)

### Ejemplo

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

### Resultado

```json
"codeSmells": [
  "SELECT * detected",
  "COMMIT inside procedure",
  "WHEN OTHERS generic exception handling"
]
```

---

## 3. Motor de Risk Score

Cada smell aporta a un score de riesgo técnico.

### Scoring actual

- SELECT * → +2
- COMMIT → +2
- WHEN OTHERS → +3
- EXECUTE IMMEDIATE → +4

### Niveles de riesgo

- 0–2 → LOW
- 3–6 → MEDIUM
- 7+ → HIGH

### Ejemplo

```json
{
  "riskScore": 11,
  "riskLevel": "HIGH"
}
```

---

## 4. Persistencia en PostgreSQL

Se almacena:

- name
- type
- procedures
- referencedTables
- codeSmells
- riskScore
- riskLevel
- sourceCode
- createdAt

Esto convierte LegacyMind en un sistema de conocimiento y no solamente en un parser.

---

## API REST

## POST Analizar Código Legacy

### Endpoint

```http
POST /api/legacy/analyze
```

### Ejemplo CURL

```bash
curl -X POST http://localhost:8080/api/legacy/analyze \
-H "Content-Type: application/json" \
-d '{
  "sourceCode": "CREATE OR REPLACE PROCEDURE generate_dynamic_report AS v_sql VARCHAR2(1000); BEGIN SELECT * FROM report_table; v_sql := '\''UPDATE audit_table SET updated_at = SYSDATE WHERE id = 1'\''; EXECUTE IMMEDIATE v_sql; COMMIT; EXCEPTION WHEN OTHERS THEN ROLLBACK; END;"
}'
```

### Respuesta

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

## GET Historial de Análisis

### Endpoint

```http
GET /api/legacy/history
```

### Ejemplo CURL

```bash
curl http://localhost:8080/api/legacy/history
```

### Respuesta

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

## Deuda Técnica Identificada

## Joins Implícitos Oracle Legacy

Caso clásico:

```sql
SELECT *
FROM customer_table c,
     debt_table d,
     audit_table a
WHERE c.id = d.customer_id
AND d.id = a.debt_id(+)
```

Actualmente el parser maneja mejor:

```sql
JOIN explícito
```

que:

```sql
tabla_a, tabla_b
```

Esto queda registrado como deuda técnica priorizada.

Porque el verdadero valor está en sintaxis vieja.

No en SQL moderno.

---

## Diferencial vs SonarQube

SonarQube pregunta:

> ¿Este código está bien escrito?

LegacyMind pregunta:

> ¿Qué hace realmente este objeto legacy y qué pasa si lo modifico?

Ese es el verdadero diferencial.

No competir como herramienta de análisis estático.

Sino como una:

# Legacy Modernization Intelligence Platform

---

## Próximos Pasos

### Corto Plazo

- functionalSummary
- mejor precisión del parser
- soporte para joins implícitos Oracle
- más code smells
- dependency mapping

### Mediano Plazo

- documentación técnica automática
- explicación funcional
- impacto de negocio
- sugerencias de modernización

### Largo Plazo

- integración con Ollama
- memoria semántica con pgvector
- estrategia de modernización asistida por IA

---

## Frase Final

LegacyMind no analiza solamente código.

Ayuda a las empresas a sobrevivir sistemas legacy.

Porque:

> el software moderno se desarrolla

> el software legacy se sobrevive
