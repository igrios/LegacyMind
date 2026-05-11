#!/bin/bash

echo "🚀 Iniciando LegacyMind..."

# BACKEND
x-terminal-emulator -e bash -c "
cd ~/Documentos/legacy-analyzer;
echo '▶ Levantando Spring Boot...';
./mvnw spring-boot:run;
exec bash
"

# FRONTEND
x-terminal-emulator -e bash -c "
cd ~/Documentos/legacy-analyzer/frontend;
echo '▶ Levantando Frontend...';
npm start;
exec bash
"

echo "✅ Backend y Frontend iniciados"
