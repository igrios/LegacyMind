# LegacyMind

LegacyMind es un micro-SaaS desarrollado en Java con Arquitectura Hexagonal enfocado en analizar, entender y modernizar sistemas legacy PL/SQL y Oracle.

El objetivo no es solamente parsear código legacy, sino descubrir:

- qué hace realmente cada package / procedure / function
- qué tablas impacta
- qué dependencias existen
- qué riesgos técnicos presenta
- qué puede romperse si se modifica
- cómo reducir el riesgo de modernización

LegacyMind transforma conocimiento tribal en conocimiento persistente.

---

# Problema que resuelve

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

# Valor Comercial

LegacyMind NO vende:

- un parser de PL/SQL

LegacyMind vende:

- reducción de incertidumbre
- menor riesgo en producción
- menor dependencia de personas clave
- mejor toma de decisiones
- modernización más segura

---

# Stack Tecnológico

## Backend

- Java 21
- Spring Boot 3
- Maven
- Spring Data JPA
- PostgreSQL
- Hibernate

## Arquitectura

- Arquitectura Hexagonal

## Desarrollo

- Git
- GitHub
- Linux Mint
- VS Code
- IntelliJ IDEA

## Futuro IA

- Ollama
- pgvector
- memoria semántica
- embeddings
- Graph-RAG
- explanation engine

---

# Estado Actual del Proyecto

Actualmente LegacyMind ya implementa:

## Parsing estructural PL/SQL

Detecta:

- PACKAGE
- PROCEDURE
- FUNCTION

Extrae:

- objectName
- objectType
- procedures internas
- functions internas
- subprograms
- referencedTables

---

# Extracción Jerárquica de Subprograms

Cada subprograma posee identidad jerárquica única:

```text
PKG_FACTURACION.SP_GENERAR_FACTURA
PKG_AUDITORIA.SP_REGISTRAR_EVENTO
