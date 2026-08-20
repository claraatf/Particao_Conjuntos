# Pasta de instâncias personalizadas

**Coloque aqui os arquivos com a sua bateria de testes.** Tudo o que estiver nesta pasta (incluindo
subpastas) será executado pelos cinco algoritmos quando o programa for iniciado com a opção
`--instancias`.

Esta pasta começa vazia de propósito: assim, ao colocar os seus arquivos, apenas eles são
executados, sem se misturarem com instâncias de exemplo. Arquivos de exemplo prontos estão em
[`../exemplos_instancias/`](../exemplos_instancias).

---

## Como executar

A partir da pasta raiz do projeto (a que contém o `pom.xml`):

**Windows (PowerShell)**

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\executar_sem_maven.ps1 -Instancias
```

**Linux / macOS / Git Bash**

```bash
bash scripts/executar_sem_maven.sh --instancias
```

As instruções completas, incluindo VS Code e IntelliJ, estão na seção
"Executando uma bateria de testes própria" do [README principal](../README.md).

---

## Formato dos arquivos

Extensões aceitas: `.txt`, `.csv`, `.in`, `.dat`.

As regras foram feitas para serem tolerantes — um arquivo com os números em qualquer disposição
funciona:

| Regra | Detalhe |
|-------|---------|
| Valores | inteiros **não negativos** (0 é permitido, negativos não) |
| Separadores | espaço, tabulação, quebra de linha, vírgula ou ponto e vírgula — pode misturar |
| Comentários | tudo depois de `#` na linha é ignorado |
| Linhas em branco | ignoradas |
| Valor máximo | 2.147.483.647 por elemento |
| Um arquivo | contém **uma** instância, a menos que use o separador abaixo |
| Várias instâncias | uma linha com três ou mais hifens (`---`) separa instâncias no mesmo arquivo |
| Nome da instância | por padrão é o nome do arquivo; use `# nome: meu_nome` para definir outro |

### Exemplo mínimo

```
10 20 30 40 50 60
```

### Exemplo com formatação livre e comentários

```
# Instância de teste do professor
# nome: caso_dificil_01

100, 200, 300
400; 500
600    700
```

Os dois blocos acima são lidos exatamente da mesma forma: o que importa são os números.

### Exemplo com várias instâncias no mesmo arquivo

```
# nome: caso_a
10 20 30 40

---

# nome: caso_b
5 5 5 5 5 5
```

---

## Onde os resultados aparecem

| Saída | Caminho |
|-------|---------|
| Tabela completa | `resultados/resultados_personalizado.csv` |
| Dashboard visual | `resultados/resultados_personalizado.html` |
| Resumo | impresso no terminal ao final |

---

## Observações importantes

- **Instâncias com mais de 26 elementos:** por segurança, os dois algoritmos exponenciais
  (Backtracking e Branch and Bound) não são executados acima desse tamanho — eles levariam tempo
  exponencial. Esses casos aparecem com status `NAO_EXECUTADO` e a explicação na coluna
  `observacao`. Para aumentar o limite, acrescente `--limite-exatos=30` (por exemplo) ao comando.
- **Tempo limite:** cada combinação de algoritmo e instância é interrompida após 30 segundos,
  registrando `TEMPO_LIMITE`. A bateria nunca trava.
- **Programação Dinâmica:** em instâncias com soma total muito alta, a tabela necessária não cabe
  na memória e o status registrado é `MEMORIA_INVIAVEL`. Isso é um resultado esperado do
  experimento, não uma falha do programa.
- **Erros de formato** indicam o arquivo, a linha e o trecho problemático.
