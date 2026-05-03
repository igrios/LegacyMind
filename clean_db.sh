#!/bin/bash

echo "🧹 Limpiando base legacymind_db..."

sudo -u postgres psql -d legacymind_db -c "TRUNCATE table_dependencies RESTART IDENTITY;"
sudo -u postgres psql -d legacymind_db -c "TRUNCATE legacy_objects RESTART IDENTITY;"

echo "✅ Base limpia"
