#!/bin/bash
# Sobe o backend real (controllers, servicos e regras) sobre repositorios em memoria, sem Maven nem banco.
# Util em ambientes sem acesso ao Maven Central. Uso: tools/local-harness/run.sh [porta]
set -e
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT="$(mktemp -d)"
javac -encoding UTF-8 -parameters -d "$OUT" $(find "$ROOT/tools/local-harness/stubs" "$ROOT/backend/src/main/java" -name "*.java") "$ROOT/tools/local-harness/Harness.java"
exec java -Duser.timezone=America/Bahia -cp "$OUT" Harness "${1:-8080}" "$ROOT/frontend/dist"
