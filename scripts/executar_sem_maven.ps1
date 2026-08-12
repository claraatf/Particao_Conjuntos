# Compila e executa o projeto usando SOMENTE o JDK (javac + java).
# Nao requer Maven instalado. Use este script se "mvn" nao funcionar na sua maquina.
#
# Uso:
#   .\scripts\executar_sem_maven.ps1              # bateria completa
#   .\scripts\executar_sem_maven.ps1 -Rapido      # bateria reduzida (~10 segundos)
#   .\scripts\executar_sem_maven.ps1 -Seed 7 -Xmx 4g

param(
    [switch]$Rapido,
    [long]$Seed = 42,
    [string]$Xmx = "4g",
    [string]$Saida = ""
)

$ErrorActionPreference = "Stop"

$raiz = Split-Path -Parent $PSScriptRoot
Set-Location $raiz

Write-Host "Diretorio do projeto: $raiz"

# 1. Verifica se o JDK esta disponivel
$javac = Get-Command javac -ErrorAction SilentlyContinue
if ($null -eq $javac) {
    Write-Host ""
    Write-Host "ERRO: 'javac' nao foi encontrado no PATH." -ForegroundColor Red
    Write-Host "Instale um JDK 17 ou superior (nao apenas o JRE) e reabra o terminal."
    Write-Host "Verifique com: javac -version"
    exit 1
}
Write-Host "JDK encontrado: $($javac.Source)"
javac -version

# 2. Compila as fontes principais
$destino = Join-Path $raiz "build\classes"
if (Test-Path $destino) { Remove-Item -Recurse -Force $destino }
New-Item -ItemType Directory -Force -Path $destino | Out-Null

$fontes = Get-ChildItem -Path (Join-Path $raiz "src\main\java") -Filter *.java -Recurse |
          ForEach-Object { $_.FullName }

if ($fontes.Count -eq 0) {
    Write-Host "ERRO: nenhum arquivo .java encontrado em src\main\java" -ForegroundColor Red
    exit 1
}

Write-Host "Compilando $($fontes.Count) arquivos..."
# Os caminhos sao passados diretamente (e nao por arquivo de argumentos) para
# funcionar mesmo quando o projeto esta em uma pasta com acentos no nome.
javac -encoding UTF-8 -d $destino $fontes
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERRO: a compilacao falhou." -ForegroundColor Red
    exit 1
}
Write-Host "Compilacao concluida."

# 3. Monta os argumentos e executa
$argumentos = @([string]$Seed)
if ($Saida -ne "") { $argumentos += $Saida }
if ($Rapido) { $argumentos += "--rapido" }

Write-Host ""
Write-Host "Executando os experimentos..."
Write-Host ""
java "-Xmx$Xmx" -cp $destino br.edu.taal.particao.Main @argumentos
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERRO: a execucao falhou." -ForegroundColor Red
    exit 1
}
