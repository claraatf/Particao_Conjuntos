#!/usr/bin/env bash
# Executa a bateria completa de experimentos e grava os resultados em CSV.
# Uso: ./scripts/executar_experimentos.sh [seed] [heap] [arquivoSaida]

set -euo pipefail

SEED="${1:-42}"
HEAP="${2:-4g}"
SAIDA="${3:-resultados/resultados.csv}"

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$RAIZ"

echo "Compilando o projeto..."
mvn -q clean package -DskipTests

JAR="target/particao-conjuntos-1.0-SNAPSHOT.jar"
if [ ! -f "$JAR" ]; then
    echo "JAR nao encontrado em $JAR. Verifique a saida do Maven." >&2
    exit 1
fi

echo "Executando experimentos (seed=$SEED, heap=$HEAP)..."
java "-Xmx$HEAP" -jar "$JAR" "$SEED" "$SAIDA"

echo "Concluido. Resultados em: $SAIDA"
