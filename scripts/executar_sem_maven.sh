#!/usr/bin/env bash
# Compila e executa o projeto usando SOMENTE o JDK (javac + java).
# Nao requer Maven instalado. Use este script se "mvn" nao funcionar na sua maquina.
#
# Uso:
#   ./scripts/executar_sem_maven.sh                 # bateria completa
#   ./scripts/executar_sem_maven.sh --rapido        # bateria reduzida (~10 segundos)
#   ./scripts/executar_sem_maven.sh --escalabilidade # limites empiricos
#   ./scripts/executar_sem_maven.sh --rapido 7      # com seed 7

set -euo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$RAIZ"

echo "Diretorio do projeto: $RAIZ"

# 1. Verifica se o JDK esta disponivel
if ! command -v javac >/dev/null 2>&1; then
    echo ""
    echo "ERRO: 'javac' nao foi encontrado no PATH." >&2
    echo "Instale um JDK 17 ou superior (nao apenas o JRE) e reabra o terminal." >&2
    echo "Verifique com: javac -version" >&2
    exit 1
fi
echo "JDK encontrado: $(command -v javac)"
javac -version

# 2. Compila as fontes principais
DESTINO="$RAIZ/build/classes"
rm -rf "$DESTINO"
mkdir -p "$DESTINO"

echo "Compilando..."
# shellcheck disable=SC2046
javac -encoding UTF-8 -d "$DESTINO" $(find src/main/java -name "*.java")
echo "Compilacao concluida."

# 3. Executa repassando todos os argumentos recebidos
echo ""
echo "Executando os experimentos..."
echo ""
java -Xmx4g -cp "$DESTINO" br.edu.taal.particao.Main "$@"
