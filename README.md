# LegacyMind

LegacyMind es un micro-SaaS orientado a la modernización de sistemas legacy en PL/SQL usando Java, arquitectura hexagonal e integración futura con IA local.

## Objetivo

El primer módulo del proyecto es:

## PL/SQL Legacy Analyzer

Este módulo permite:

- Analizar packages y objetos PL/SQL
- Detectar dependencias iniciales
- Identificar procedures internas
- Detectar tablas referenciadas
- Generar documentación técnica automática
- Asistir migraciones hacia arquitectura moderna
- Preparar integración futura con IA local (Ollama)
- Persistir conocimiento técnico con PostgreSQL + pgvector

---

# Stack Tecnológico

## Backend

- Java 21
- Spring Boot 3.5.13
- Maven
- Arquitectura Hexagonal

## Base de Datos

- PostgreSQL
- pgvector (vector database para memoria semántica)

## IA futura

- Ollama (modelo local)

## IDE

- Visual Studio Code
- IntelliJ IDEA

---

# Endpoint disponible

## Analizar código PL/SQL

POST /api/legacy/analyze

### Request

```json
{
  "sourceCode": "CREATE OR REPLACE PACKAGE customer_pkg AS PROCEDURE validate_customer; END;"
}
```

### Response actual

```json
{
  "name": "UNKNOWN_OBJECT",
  "type": "UNKNOWN",
  "procedures": [],
  "referencedTables": []
}
```

---

# Probar con curl

```bash
curl -X POST http://localhost:8080/api/legacy/analyze \
-H "Content-Type: application/json" \
-d '{
  "sourceCode": "CREATE OR REPLACE PACKAGE customer_pkg AS PROCEDURE validate_customer; END;"
}'
```

---

# Repositorio

https://github.com/igrios/LegacyMind
