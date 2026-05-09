#!/bin/bash

echo "🧹 Limpiando base legacymind_db..."

sudo -u postgres psql -d legacymind_db -c "TRUNCATE table_dependencies RESTART IDENTITY CASCADE;"

sudo -u postgres psql -d legacymind_db -c "TRUNCATE legacy_objects RESTART IDENTITY CASCADE;"

sudo -u postgres psql -d legacymind_db -c "TRUNCATE knowledge_relation_entity RESTART IDENTITY CASCADE;"

echo "✅ Base limpia completamente"
