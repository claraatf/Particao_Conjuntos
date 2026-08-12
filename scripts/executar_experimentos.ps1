# Executa a bateria completa de experimentos e grava os resultados em CSV.
# Uso: .\scripts\executar_experimentos.ps1 [-Seed 42] [-Xmx 4g]

param(
    [long]$Seed = 42,
    [string]$Xmx = "4g",
    [string]$Saida = "resultados/resultados.csv"
)

$ErrorActionPreference = "Stop"
$raiz = Split-Path -Parent $PSScriptRoot
Set-Location $raiz

Write-Host "Compilando o projeto..."
mvn -q clean package -DskipTests

$jar = "target/particao-conjuntos-1.0-SNAPSHOT.jar"
if (-not (Test-Path $jar)) {
    throw "JAR nao encontrado em $jar. Verifique a saida do Maven."
}

Write-Host "Executando experimentos (seed=$Seed, heap=$Xmx)..."
java "-Xmx$Xmx" -jar $jar $Seed $Saida

Write-Host "Concluido. Resultados em: $Saida"
