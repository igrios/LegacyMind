# LegacyMind

LegacyMind es un micro-SaaS construido en Java con Arquitectura Hexagonal enfocado en analizar y modernizar sistemas legacy en PL/SQL.

El objetivo no es solo parsear código legacy de Oracle, sino entender impacto de negocio, riesgos técnicos, dependencias y caminos de modernización.

## Visión Principal

LegacyMind transforma conocimiento tribal en conocimiento técnico persistente, consultable y accionable.

Ayuda a reducir el riesgo de modernización en entornos Oracle PL/SQL.

## Módulo Actual: PL/SQL Legacy Analyzer

Permite:
- Analizar Packages, Procedures y Functions
- Detectar nombre y tipo del objeto
- Detectar procedures internas
- Detectar tablas referenciadas
- Detectar code smells legacy
- Calcular risk score técnico
- Clasificar nivel de riesgo (LOW / MEDIUM / HIGH)
- Persistir análisis en PostgreSQL
- Consultar historial de análisis

## Stack

- Java 21
- Spring Boot 3.5.13
- Maven
- Spring Data JPA
- PostgreSQL
- Arquitectura Hexagonal
- Git + GitHub
- Futuro: Ollama + pgvector

## Project Structure

src/main/java/com/ignacio/legacyanalyzer
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
│   │   ├── input/controller
│   │   └── output
│   │       ├── parser
│   │       └── persistence
│   │
│   └── config
│
└── LegacyAnalyzerApplication.java


## Code Smells detectados

- SELECT *
- COMMIT inside procedure
- WHEN OTHERS generic exception handling
- Dynamic SQL (EXECUTE IMMEDIATE)

# LegacyMind\n\nREADME completo con arquitectura, ejemplos de POST/GET, code smells, risk score y visión comercial de LegacyMind.\n\n## Endpoints principales\n\n### POST /api/legacy/analyze\nAnaliza código PL/SQL legacy, detecta tablas, code smells y calcula riesgo.\n\n### GET /api/legacy/history\nDevuelve historial persistido en PostgreSQL.\n\n## Code smells detectados\n- SELECT *\n- COMMIT inside procedure\n- WHEN OTHERS\n- EXECUTE IMMEDIATE\n\n## Risk Score\n- SELECT * = +2\n- COMMIT = +2\n- WHEN OTHERS = +3\n- EXECUTE IMMEDIATE = +4\n\n0-2 LOW\n3-6 MEDIUM\n7+ HIGH\n\n## Valor\nLegacyMind vende reducción de riesgo en modernización legacy y transforma conocimiento tribal en conocimiento persistente.\n

## Risk Score actual

- SELECT * → +2
- COMMIT → +2
- WHEN OTHERS → +3
- EXECUTE IMMEDIATE → +4

0–2 → LOW
3–6 → MEDIUM
7+ → HIGH

## Valor Comercial

LegacyMind no vende un parser de PL/SQL.

Vende reducción de riesgo en modernización legacy.

Especialmente útil para bancos, telecom, seguros, fintech y grandes empresas con Oracle legacy.

## Próximos pasos

- Functional summary generation
- Mejor precisión del parser
- Mejor soporte para sintaxis Oracle legacy
- Más code smells
- Dependency mapping
- Integración con Ollama
- Memoria semántica con pgvector

## Frase clave

LegacyMind convierte conocimiento tribal en conocimiento persistente.




Repository

GitHub:

https://github.com/igrios/LegacyMind
