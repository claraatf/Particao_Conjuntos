# Problema da Partição de Conjuntos — Estudo Comparativo de Técnicas de Projeto de Algoritmos

Projeto da disciplina de Técnicas de Análise e Algoritmos (TAAL).

## O problema

Dado um conjunto de números inteiros positivos, dividi-lo em dois subconjuntos cujas somas sejam
iguais ou apresentem a **menor diferença possível**. É um problema NP-difícil (versão de otimização
do *Partition Problem*), o que torna interessante comparar estratégias exatas e heurísticas.

## Algoritmos implementados

| # | Estratégia | Classe | Exato? | Complexidade teórica |
|---|-----------|--------|:------:|----------------------|
| 1 | Backtracking | `BacktrackingPartition` | Sim | O(2ⁿ) tempo, O(n) espaço |
| 2 | Branch and Bound | `BranchAndBoundPartition` | Sim | O(2ⁿ) pior caso, O(n) espaço |
| 3 | Programação Dinâmica | `DynamicProgrammingPartition` | Sim | O(n·S) tempo e espaço (pseudo-polinomial) |
| 4 | Guloso (LPT) | `GreedyPartition` | Não | O(n log n) |
| 5 | Karmarkar-Karp (diferenças) | `KarmarkarKarpPartition` | Não | O(n log n) |

Onde `S` é a soma total dos elementos.

### Observações sobre cada estratégia

- **Backtracking** — explora a árvore binária completa de decisões (cada elemento vai para A ou B).
  Usa quebra de simetria (o primeiro elemento fica sempre em A) e parada antecipada ao encontrar
  diferença zero, mas não usa função de *bound*.
- **Branch and Bound** — mesma árvore, porém com elementos ordenados de forma decrescente e poda por
  limite inferior: se `|somaA − somaB| − somaRestante ≥ melhorDiferença`, o ramo é descartado.
- **Programação Dinâmica** — reduz o problema a *Subset Sum*: encontra a maior soma alcançável até
  `S/2`. Mantém a tabela completa para reconstruir a partição, e não apenas o valor ótimo. Por ser
  pseudo-polinomial, torna-se inviável em memória quando `S` é grande — o experimento registra isso
  explicitamente via `TabelaInviavelException`.
- **Guloso** — ordena decrescente e coloca cada elemento no subconjunto de menor soma atual.
- **Karmarkar-Karp** — retira repetidamente os dois maiores valores e os substitui pela diferença,
  o que equivale a decidir que ficarão em lados opostos. A partição é reconstruída bicolorindo, por
  busca em largura, a árvore de restrições formada.

## Métricas coletadas

Para cada execução (`Metrics` e `ExecutionRecord`):

- tempo de execução (mediana de repetições, após *warm-up* da JVM);
- consumo de memória;
- número de estados explorados;
- número de chamadas recursivas;
- número de podas realizadas;
- profundidade máxima da árvore de busca;
- diferença encontrada, diferença ótima de referência, GAP percentual e se atingiu o ótimo;
- status: `SUCESSO`, `TEMPO_LIMITE`, `MEMORIA_INVIAVEL` ou `ERRO`.

## Instâncias de teste

`InstanceGenerator` produz cinco perfis, todos reprodutíveis a partir de uma *seed*:

| Perfil | Característica | Por que existe |
|--------|----------------|----------------|
| `UNIFORME_PEQUENO` | valores em [1, 100] | muitas partições perfeitas existem |
| `UNIFORME_GRANDE` | valores em [1, 10⁶] | dificulta atingir diferença zero |
| `VALORES_ENORMES` | valores em [10⁶, 10⁸] | torna a tabela de PD inviável |
| `PARTICAO_PERFEITA` | diferença zero garantida por construção | mede corretude |
| `DOMINANTE` | um valor maior que a soma dos demais | caso clássico de dificuldade para o guloso |

Cada perfil é combinado com vários tamanhos e 5 variações, formando a bateria completa
(~1000 execuções por rodada).

## Como executar

### No IntelliJ IDEA

1. `File > Open` e selecione a pasta do projeto (o IntelliJ detecta o `pom.xml` automaticamente).
2. Aguarde a importação das dependências Maven.
3. Abra `src/main/java/br/edu/taal/particao/Main.java` e clique em **Run**.
4. Os resultados vão para o console e para `resultados/resultados.csv`.

Recomenda-se aumentar a memória da JVM em `Run > Edit Configurations > VM options`:

```
-Xmx4g
```

### Pela linha de comando

```bash
mvn clean package
```

```bash
java -Xmx4g -jar target/particao-conjuntos-1.0-SNAPSHOT.jar 42 resultados/resultados.csv
```

Os dois argumentos são opcionais: a *seed* (padrão `42`) e o arquivo de saída
(padrão `resultados/resultados.csv`). Usar a mesma seed reproduz exatamente a mesma bateria.

### Testes

```bash
mvn test
```

Os testes verificam corretude: preservação da soma total, concordância entre os três algoritmos
exatos, heurísticas nunca superando o ótimo e reprodutibilidade do gerador.

## Estrutura do projeto

```
src/main/java/br/edu/taal/particao/
├── Main.java                          # bateria de experimentos e relatório no console
├── model/
│   ├── Instance.java                  # instância do problema
│   ├── Metrics.java                   # métricas de uma execução
│   └── PartitionResult.java           # resultado + cálculo de GAP
├── algorithms/
│   ├── PartitionAlgorithm.java        # contrato comum
│   ├── BacktrackingPartition.java
│   ├── BranchAndBoundPartition.java
│   ├── DynamicProgrammingPartition.java
│   ├── GreedyPartition.java
│   ├── KarmarkarKarpPartition.java
│   ├── TabelaInviavelException.java
│   └── TempoLimiteExcedidoException.java
└── experiment/
    ├── InstanceGenerator.java         # geração reprodutível de instâncias
    ├── ExperimentRunner.java          # execução com warm-up e tempo limite
    ├── ExecutionRecord.java           # uma linha da tabela de resultados
    └── CsvWriter.java                 # exportação para análise
```

## Saída em CSV

O arquivo gerado tem uma linha por (instância × algoritmo), com as colunas:

```
perfil, tamanho, instancia, algoritmo, exato, status, soma_a, soma_b, diferenca,
diferenca_referencia, gap_percentual, atingiu_otimo, tempo_ms, memoria_mb,
estados_explorados, chamadas_recursivas, podas, profundidade_maxima, observacao
```

Basta importar em uma planilha ou ferramenta estatística para montar as tabelas e gráficos do
relatório técnico.

## Reprodutibilidade

Para que os experimentos sejam reprodutíveis, registre no relatório: modelo do processador, memória
RAM, sistema operacional, versão do JDK, valor de `-Xmx` e a *seed* utilizada. As três primeiras
linhas impressas pelo programa já reportam parte dessas informações.
