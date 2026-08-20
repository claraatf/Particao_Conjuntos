# Problema da Partição de Conjuntos — Estudo Comparativo de Técnicas de Projeto de Algoritmos

Projeto da disciplina de Técnicas de Análise e Algoritmos (TAAL).

> **Para quem vai avaliar o projeto:** as instruções de execução estão na seção
> [Como executar](#como-executar), com passo a passo completo para **VS Code**, **IntelliJ IDEA** e
> **linha de comando**. Existe um caminho de execução que **não exige Maven instalado** —
> apenas o JDK. Se algo falhar, a seção [Solução de problemas](#solução-de-problemas) cobre os
> erros mais comuns com a mensagem exata e o que fazer.
>
> **Para submeter uma bateria de testes própria**, vá direto para
> [Executando uma bateria de testes própria](#executando-uma-bateria-de-testes-própria): basta
> copiar os arquivos `.txt` para a pasta `instancias/` e executar um único comando. Também há uma **interface gráfica Swing**
> opcional para configurar a bateria, acompanhar o progresso e abrir os resultados.

---

## Índice

1. [O problema](#o-problema)
2. [Algoritmos implementados](#algoritmos-implementados)
3. [Pré-requisito único: o JDK](#pré-requisito-único-o-jdk)
4. [Como executar](#como-executar)
   - [Opção A — VS Code](#opção-a--vs-code-passo-a-passo)
   - [Opção B — IntelliJ IDEA](#opção-b--intellij-idea-passo-a-passo)
   - [Opção C — Linha de comando sem Maven](#opção-c--linha-de-comando-sem-maven-mais-simples)
   - [Opção D — Linha de comando com Maven](#opção-d--linha-de-comando-com-maven)
5. [**Executando uma bateria de testes própria**](#executando-uma-bateria-de-testes-própria)
6. [O que você deve ver ao executar](#o-que-você-deve-ver-ao-executar)
7. [Executando os testes automatizados](#executando-os-testes-automatizados)
8. [Solução de problemas](#solução-de-problemas)
9. [Métricas coletadas](#métricas-coletadas)
10. [Instâncias de teste](#instâncias-de-teste)
11. [Estrutura do projeto](#estrutura-do-projeto)
12. [Saídas em CSV e HTML](#saídas-em-csv-e-html)
13. [Reprodutibilidade](#reprodutibilidade)
14. [Declaração de uso de IA](#declaração-de-uso-de-ia)

---

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
  explicitamente com o status `MEMORIA_INVIAVEL` em vez de derrubar o programa.
- **Guloso** — ordena decrescente e coloca cada elemento no subconjunto de menor soma atual.
- **Karmarkar-Karp** — retira repetidamente os dois maiores valores e os substitui pela diferença,
  o que equivale a decidir que ficarão em lados opostos. A partição é reconstruída bicolorindo, por
  busca em largura, a árvore de restrições formada.

---

## Pré-requisito único: o JDK

O projeto precisa do **JDK 17 ou superior**. Não é necessário instalar Maven (veja a
[Opção C](#opção-c--linha-de-comando-sem-maven-mais-simples)).

Versões mais novas do JDK também funcionam — o projeto foi executado com sucesso nas versões 17 e
26. Em JDKs recentes, o IntelliJ emite alguns avisos `WARNING: A restricted method ... has been
called`, que vêm do agente da própria IDE e não do código do projeto.

### Como verificar se você já tem o JDK

Abra um terminal (PowerShell, Prompt de Comando ou Terminal do Linux/macOS) e execute:

```bash
javac -version
```

- Se aparecer algo como `javac 17.0.12` (ou número maior), **está tudo certo**, pule para
  [Como executar](#como-executar).
- Se aparecer `javac não é reconhecido` / `command not found`, siga a instalação abaixo.

> **Atenção:** `java -version` funcionar **não** é suficiente. É preciso que `javac -version`
> também funcione, pois `java` sozinho indica apenas o JRE, que não compila código.

### Como instalar o JDK 17

**Windows**

1. Baixe o instalador em <https://adoptium.net/temurin/releases/?version=17> escolhendo
   *Operating System: Windows*, *Architecture: x64*, *Package Type: JDK*, arquivo `.msi`.
2. Execute o instalador. Na tela de opções, marque **"Set JAVA_HOME variable"** e
   **"Add to PATH"** (por padrão vêm desabilitadas).
3. **Feche e reabra o terminal** (a alteração de PATH só vale em terminais novos).
4. Confirme com `javac -version`.

**Linux (Ubuntu/Debian)**

```bash
sudo apt update && sudo apt install openjdk-17-jdk
```

**macOS (com Homebrew)**

```bash
brew install --cask temurin@17
```

---

## Como executar

Escolha **uma** das quatro opções abaixo. Todas produzem o mesmo resultado.

O programa tem três modos:

| Modo | Como acionar | Duração | Para que serve |
|------|--------------|---------|----------------|
| **Rápido** | argumento `--rapido` | ~10 segundos | confirmar que o ambiente funciona |
| **Completo** | sem argumentos | 1 a 5 minutos | gerar os dados do relatório |
| **Escalabilidade** | argumento `--escalabilidade` | variável | localizar limites práticos com timeout adaptativo |

> **Sugestão:** rode primeiro no modo rápido. Depois use o completo para os dados comparativos e o
> de escalabilidade para observar quando cada estratégia deixa de ser viável.

### Interface gráfica Swing

A interface é opcional e usa exatamente o mesmo executor da linha de comando. Ela permite escolher
o modo, a seed e o CSV de saída, acompanha o log sem travar a janela e, ao terminar, oferece botões
para abrir o dashboard e a pasta dos resultados. Não requer nenhuma biblioteca além do próprio JDK.

Depois de gerar o JAR com `mvn clean package`, abra a interface com:

```bash
java -Xmx4g -jar target/particao-conjuntos-1.0-SNAPSHOT.jar --gui
```

Sem Maven, no Windows:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\executar_sem_maven.ps1 -Interface
```

No Linux, macOS ou Git Bash:

```bash
bash scripts/executar_sem_maven.sh --gui
```

---

### Opção A — VS Code (passo a passo)

**Passo 1 — Instalar a extensão de Java**

1. Abra o VS Code.
2. Clique no ícone de **Extensões** na barra lateral esquerda (ou pressione `Ctrl+Shift+X`).
3. Digite na busca: `Extension Pack for Java`.
4. Instale o pacote publicado pela **Microsoft** (identificador `vscjava.vscode-java-pack`).
   Ele já inclui tudo o que é necessário: suporte a Java, depurador, Maven e testes.
5. Aguarde a instalação terminar (aparece uma notificação no canto inferior direito).

> Este projeto já contém um arquivo `.vscode/extensions.json`, então o VS Code também deve sugerir
> essa extensão automaticamente ao abrir a pasta.

**Passo 2 — Abrir o projeto**

1. Menu **File > Open Folder...** (`Arquivo > Abrir Pasta...`).
2. Selecione a pasta **`TAAL`** — a pasta que contém o arquivo `pom.xml`.
   *Importante:* selecione a pasta em si, e não uma subpasta como `src`.
3. Se o VS Code perguntar *"Do you trust the authors of the files in this folder?"*, clique em
   **Yes, I trust the authors**. Sem isso o Java não é carregado.

**Passo 3 — Aguardar a importação**

Na barra inferior aparecerá um indicador de progresso (`Importing Java projects...` ou um ícone
girando). **Aguarde até ele desaparecer** — na primeira vez pode levar de 1 a 3 minutos, porque a
extensão baixa as dependências.

> Você **não** precisa ter o Maven instalado: a extensão de Java do VS Code traz um Maven embutido.

**Passo 4 — Executar**

Existem duas formas. A primeira é a mais direta:

*Forma 1 — pelo painel Run and Debug (recomendada)*

1. Clique no ícone **Run and Debug** na barra lateral (ou pressione `Ctrl+Shift+D`).
2. No menu suspenso no topo do painel, escolha **"1. Bateria RAPIDA (cerca de 10 segundos)"**.
   Essas configurações já vêm prontas no arquivo `.vscode/launch.json`.
3. Clique no botão verde ▶ (ou pressione `F5`).
4. A saída aparece na aba **DEBUG CONSOLE**, na parte inferior da tela.
5. Para a bateria completa, repita escolhendo **"2. Bateria COMPLETA (1 a 5 minutos)"**.
6. Para localizar os limites práticos, escolha **"3. ESCALABILIDADE (limites empiricos)"**.
7. Para usar a janela, escolha **"4. INTERFACE GRAFICA"**.

*Forma 2 — pelo botão Run acima do método `main`*

1. No explorador de arquivos, abra
   `src/main/java/br/edu/taal/particao/Main.java`.
2. Logo acima da linha `public static void main(String[] args)` aparecerá um link **Run | Debug**.
3. Clique em **Run**. Isso executa a **bateria completa** (sem o argumento `--rapido`).
4. A saída aparece na aba **TERMINAL**.

**Passo 5 — Conferir o resultado**

Ao final, o programa informa onde gravou o CSV, por exemplo:

```
Resultados gravados em: C:\...\TAAL\resultados\resultados_rapido.csv
Dashboard gravado em: C:\...\TAAL\resultados\resultados_rapido_dashboard.html
```

Os dois arquivos aparecerão na pasta `resultados/` dentro do projeto. Dê dois cliques no arquivo
HTML para abrir o dashboard no navegador; ele não requer internet nem servidor local.

---

### Opção B — IntelliJ IDEA (passo a passo)

**Passo 1 — Abrir o projeto**

1. Na tela inicial do IntelliJ, clique em **Open** (ou menu **File > Open...**).
2. Selecione a pasta **`TAAL`** — a que contém o `pom.xml` — e clique em **OK**.
3. Se aparecer a caixa *"Trust and Open Project?"*, clique em **Trust Project**.

**Passo 2 — Aguardar a importação do Maven**

1. O IntelliJ detecta o `pom.xml` automaticamente e começa a importar.
2. Na barra inferior aparece *"Resolving Maven dependencies..."*. **Aguarde terminar**
   (1 a 3 minutos na primeira vez).
3. Se aparecer uma notificação *"Maven build script found"* com o botão **Load Maven Project** ou
   **Load**, clique nele.

> O IntelliJ possui um Maven embutido, então não é necessário instalar Maven separadamente.

**Passo 3 — Verificar o JDK do projeto**

1. Menu **File > Project Structure...** (`Ctrl+Alt+Shift+S`).
2. Em **Project Settings > Project**, confira o campo **SDK**.
3. Se estiver vazio ou marcado com `<No SDK>`, clique no campo, escolha **Add SDK > Download JDK...**,
   selecione a versão **17** e clique em **Download**.
4. Confirme que **Language level** está em **17** ou superior.
5. Clique em **OK**.

**Passo 4 — Executar**

1. No painel de projeto à esquerda, navegue até
   `src/main/java/br/edu/taal/particao/Main.java` e dê um duplo clique para abrir.
2. Clique no **triângulo verde ▶** ao lado da linha `public class Main` (ou ao lado do método
   `main`) e escolha **Run 'Main.main()'**.
3. A saída aparece na janela **Run**, na parte inferior.

Isso executa a **bateria completa**. Para rodar a bateria rápida:

1. Menu **Run > Edit Configurations...**.
2. Selecione a configuração **Main** na lista à esquerda.
3. No campo **Program arguments**, digite: `--rapido`
4. No campo **VM options**, digite: `-Xmx4g`
   *(Se o campo VM options não estiver visível, clique em **Modify options > Add VM options**.)*
5. Clique em **OK** e execute novamente com ▶.

Para o modo de escalabilidade, use `--escalabilidade` no campo **Program arguments** no lugar de
`--rapido`.

Para abrir a interface gráfica pelo IntelliJ, use `--gui` em **Program arguments**.

---

### Opção C — Linha de comando sem Maven (mais simples)

Esta opção precisa **apenas do JDK**. Use se as opções anteriores derem problema.

**Windows (PowerShell)**

1. Abra o **PowerShell**.
2. Navegue até a pasta do projeto. Exemplo:

```powershell
cd "C:\Users\SeuUsuario\Desktop\TAAL"
```

3. Execute a bateria rápida:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\executar_sem_maven.ps1 -Rapido
```

4. Para a bateria completa, basta omitir `-Rapido`:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\executar_sem_maven.ps1
```

5. Para procurar os limites empíricos dos algoritmos:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\executar_sem_maven.ps1 -Escalabilidade
```

> O trecho `-ExecutionPolicy Bypass` evita o erro *"a execução de scripts foi desabilitada neste
> sistema"*, comum em instalações padrão do Windows.

**Linux / macOS / Git Bash**

```bash
bash scripts/executar_sem_maven.sh --rapido
```

```bash
bash scripts/executar_sem_maven.sh
```

```bash
bash scripts/executar_sem_maven.sh --escalabilidade
```

O script verifica se o JDK está instalado, compila todas as classes-fonte em `build/classes` e executa o
programa, exibindo mensagens claras em cada etapa.

**Se preferir digitar os comandos manualmente**, sem usar o script:

*Windows (PowerShell), a partir da pasta do projeto:*

```powershell
javac -encoding UTF-8 -d build\classes (Get-ChildItem -Path src\main\java -Filter *.java -Recurse | ForEach-Object { $_.FullName })
```

```powershell
java -Xmx4g -cp build\classes br.edu.taal.particao.Main --rapido
```

*Linux / macOS / Git Bash:*

```bash
javac -encoding UTF-8 -d build/classes $(find src/main/java -name "*.java")
```

```bash
java -Xmx4g -cp build/classes br.edu.taal.particao.Main --rapido
```

---

### Opção D — Linha de comando com Maven

Use apenas se você **já tem o Maven instalado** (verifique com `mvn -version`).

```bash
mvn clean package
```

```bash
java -Xmx4g -jar target/particao-conjuntos-1.0-SNAPSHOT.jar --rapido
```

Para a bateria completa, omita o `--rapido`:

```bash
java -Xmx4g -jar target/particao-conjuntos-1.0-SNAPSHOT.jar
```

Para executar apenas o estudo de escalabilidade:

```bash
java -Xmx4g -jar target/particao-conjuntos-1.0-SNAPSHOT.jar --escalabilidade
```

Também existem scripts que fazem as duas etapas de uma vez:

```bash
bash scripts/executar_experimentos.sh
```

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\executar_experimentos.ps1
```

### Argumentos aceitos pelo programa

Todos são opcionais e podem ser combinados em qualquer ordem:

| Argumento | Significado | Padrão |
|-----------|-------------|--------|
| `--rapido` | executa a bateria reduzida | ausente (bateria completa) |
| `--escalabilidade` | executa tamanhos graduais com interrupção adaptativa | ausente |
| `--instancias` | executa os arquivos da pasta `instancias/` | ausente |
| `--instancias=<caminho>` | executa os arquivos de outra pasta ou um arquivo específico | ausente |
| `--limite-exatos=<n>` | tamanho máximo em que os algoritmos exponenciais são executados | `26` |
| `--gui` | abre a interface gráfica Swing | ausente |
| primeiro número | *seed* do gerador de instâncias | `42` |
| segundo valor | caminho do arquivo CSV de saída | `resultados/resultados.csv` |

`--rapido`, `--escalabilidade` e `--instancias` são mutuamente exclusivos. `--gui` deve ser usado
isoladamente, pois modo, seed e saída são escolhidos na janela. Quando nenhum caminho é informado, o
modo de escalabilidade grava `resultados/resultados_escalabilidade.csv`.

Exemplo com todos os argumentos:

```bash
java -Xmx4g -cp build/classes br.edu.taal.particao.Main 7 saida/meu_teste.csv --rapido
```

---

## Executando uma bateria de testes própria

Esta seção é destinada a quem quer submeter os cinco algoritmos a **instâncias próprias**, e não às
geradas automaticamente pelo projeto. Os passos abaixo são autocontidos: basta segui-los na ordem.

Há duas formas, e ambas levam ao mesmo resultado. A **Forma 1** é a recomendada.

---

### Forma 1 — Colocar os arquivos na pasta `instancias/`

**Passo 1.** Abra a pasta do projeto (a que contém o arquivo `pom.xml`). Dentro dela já existe uma
pasta chamada **`instancias`**. Ela vem vazia de propósito, contendo apenas um `README.md`.

**Passo 2.** Copie para dentro de `instancias/` os arquivos com os seus conjuntos de teste. Cada
arquivo deve ter extensão **`.txt`** (também são aceitas `.csv`, `.in` e `.dat`) e conter os números
da instância. O formato completo está descrito em [Formato dos arquivos](#formato-dos-arquivos-de-instância)
logo abaixo; no caso mais simples, basta uma linha com os números separados por espaço:

```
10 20 30 40 50 60
```

Pode colocar quantos arquivos quiser, inclusive organizados em subpastas — todos serão lidos.

**Passo 3.** Execute o comando correspondente ao seu sistema, **a partir da pasta raiz do projeto**:

*Windows (PowerShell):*

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\executar_sem_maven.ps1 -Instancias
```

*Linux, macOS ou Git Bash:*

```bash
bash scripts/executar_sem_maven.sh --instancias
```

O script verifica o JDK, compila o projeto e executa a bateria. Não é necessário ter Maven.

**Passo 4.** Confira os resultados:

| O que | Onde |
|-------|------|
| Resumo por algoritmo | impresso no terminal, ao final |
| Tabela completa com todas as métricas | `resultados/resultados_personalizado.csv` |
| Dashboard visual (abrir no navegador) | `resultados/resultados_personalizado_dashboard.html` |

---

### Forma 2 — Apontar para um arquivo ou pasta em qualquer lugar

Use esta forma se preferir manter os arquivos fora do projeto — por exemplo, na Área de Trabalho.

Funciona tanto com **uma pasta** quanto com **um único arquivo `.txt`**.

*Windows (PowerShell):*

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\executar_sem_maven.ps1 -CaminhoInstancias "C:\Users\SeuUsuario\Desktop\meus_testes"
```

*Linux, macOS ou Git Bash:*

```bash
bash scripts/executar_sem_maven.sh --instancias=/caminho/para/meus_testes
```

Para um único arquivo, informe o caminho do arquivo em vez da pasta:

```bash
bash scripts/executar_sem_maven.sh --instancias=/caminho/para/bateria.txt
```

> Se o caminho tiver espaços, mantenha-o entre aspas.

---

### Formato dos arquivos de instância

O leitor foi feito para ser tolerante: **o que importa são os números**, e a disposição deles no
arquivo é irrelevante.

| Regra | Detalhe |
|-------|---------|
| Valores | inteiros **não negativos** (`0` é aceito; negativos são recusados) |
| Separadores | espaço, tabulação, quebra de linha, vírgula ou ponto e vírgula — podem ser misturados |
| Comentários | tudo que vier depois de `#` na linha é ignorado |
| Linhas em branco | ignoradas |
| Valor máximo | 2.147.483.647 por elemento |
| Instâncias por arquivo | uma, a menos que use o separador `---` |
| Várias instâncias | uma linha com três ou mais hifens (`---`) separa instâncias no mesmo arquivo |
| Nome nos relatórios | é o nome do arquivo; para definir outro, use `# nome: meu_nome` |

**Exemplo 1 — o mais simples possível** (arquivo `caso1.txt`):

```
10 20 30 40 50 60
```

**Exemplo 2 — formatação livre, com comentários:**

```
# Instância de teste
# nome: caso_dificil_01

100, 200, 300
400; 500
600    700   # comentário no fim da linha
```

**Exemplo 3 — três instâncias em um único arquivo:**

```
# nome: caso_a
10 20 30 40

---

# nome: caso_b
5 5 5 5 5 5

---

# nome: caso_c
1000000 999999 1000001 999998
```

Arquivos de exemplo prontos para usar estão em **`exemplos_instancias/`**. Para executá-los e ver
como funciona antes de usar os seus próprios arquivos:

```bash
bash scripts/executar_sem_maven.sh --instancias=exemplos_instancias
```

---

### Executando pelo VS Code ou IntelliJ

Se preferir usar a IDE em vez do terminal:

**VS Code:** coloque os arquivos em `instancias/`, abra o painel **Run and Debug** (`Ctrl+Shift+D`),
escolha a configuração **"5. INSTANCIAS PERSONALIZADAS"** e pressione `F5`.

**IntelliJ:** coloque os arquivos em `instancias/`, vá em **Run > Edit Configurations...**, selecione
a configuração `Main`, escreva `--instancias` no campo **Program arguments**, clique em **OK** e
execute com o botão ▶.

---

### O que esperar da execução

Para cada instância, os cinco algoritmos são executados e uma linha por algoritmo é impressa:

```
Instancia caso_a                           (n=    8, soma=146)
   Backtracking           diferenca=0            tempo=    0.021 ms  estados=93           gap=0.00%
   BranchAndBound         diferenca=0            tempo=    0.018 ms  estados=61           gap=0.00%
   ProgramacaoDinamica    diferenca=0            tempo=    0.033 ms  estados=592          gap=0.00%
   Guloso                 diferenca=0            tempo=    0.005 ms  estados=8            gap=0.00%
   KarmarkarKarp          diferenca=0            tempo=    0.024 ms  estados=7            gap=0.00%
```

Três situações são **resultados esperados do experimento, e não falhas do programa**:

| O que aparece | O que significa |
|---------------|-----------------|
| `NAO_EXECUTADO` | a instância tem mais de 26 elementos e o algoritmo é exponencial (veja abaixo) |
| `TEMPO_LIMITE` | o algoritmo passou de 30 segundos naquela instância e foi interrompido |
| `MEMORIA_INVIAVEL` | a tabela da Programação Dinâmica não caberia na memória (soma total muito alta) |

**Sobre instâncias com mais de 26 elementos:** Backtracking e Branch and Bound são exponenciais, e
acima desse tamanho consumiriam o tempo limite sem produzir informação nova. Por isso são pulados
por padrão, com o motivo registrado na coluna `observacao` do CSV. Para forçar a execução em
instâncias maiores, acrescente `--limite-exatos=<n>`:

```bash
bash scripts/executar_sem_maven.sh --instancias --limite-exatos=32
```

*No PowerShell, o parâmetro equivalente é `-LimiteExatos 32`.*

Os três algoritmos restantes (Programação Dinâmica, Guloso e Karmarkar-Karp) são executados em
qualquer tamanho, respeitando o tempo limite e a memória disponível.

---

### Se algo der errado

O programa valida os arquivos **antes** de começar e, em caso de problema, informa o arquivo, a
linha e o trecho exato, encerrando com código de saída `1`:

```
ERRO: Erro no arquivo C:\...\bateria.txt, linha 2: "abc" nao e um numero inteiro. Use # para
comentarios e separe os valores por espaco, virgula ou quebra de linha.

Consulte instancias/README.md para o formato aceito.
```

| Mensagem | Causa | Solução |
|----------|-------|---------|
| `Nenhum arquivo de instancia encontrado` | a pasta está vazia ou os arquivos têm outra extensão | renomeie os arquivos para `.txt` |
| `Caminho de instancias nao encontrado` | o caminho informado não existe | verifique o caminho; use aspas se houver espaços |
| `"..." nao e um numero inteiro` | há texto onde deveria haver número (por exemplo, um cabeçalho de CSV) | remova a linha ou comece-a com `#` |
| `valor negativo` | há número negativo no arquivo | o problema da partição exige inteiros não negativos |
| `nao contem nenhum numero` | o arquivo só tem comentários ou está vazio | inclua ao menos um número |

Se colocar arquivos em `instancias/` e executar **sem** a opção `--instancias`, o programa avisa no
início que eles não serão executados e lembra qual opção usar.

---

## O que você deve ver ao executar

O programa imprime o ambiente, o progresso instância por instância e, ao final, um resumo.
A execução no modo rápido termina em cerca de 10 segundos com uma saída semelhante a esta:

```
=== Problema da Particao de Conjuntos - Estudo Comparativo ===
Modo: RAPIDO (bateria reduzida, cerca de 10 segundos)
Seed: 42
Repeticoes por execucao: 1 aquecimento(s) + 3 medicao(oes)
Tempo limite por combinacao: 30s
Ambiente: Java 17.0.12 | Windows 11 | processadores disponiveis: 12 | memoria maxima da JVM: 4018 MB

Instancia uniforme_pequeno_n10_v0          (n=   10, soma=490)
   Backtracking           diferenca=0            tempo=    0.030 ms  estados=123          gap=0.00%
   BranchAndBound         diferenca=0            tempo=    0.045 ms  estados=160          gap=0.00%
   ProgramacaoDinamica    diferenca=0            tempo=    0.447 ms  estados=2460         gap=0.00%
   Guloso                 diferenca=0            tempo=    0.009 ms  estados=10           gap=0.00%
   KarmarkarKarp          diferenca=0            tempo=    0.074 ms  estados=9            gap=0.00%
   ...

Bateria concluida em 10.4 segundos (250 registros).
Resultados gravados em: ...\TAAL\resultados\resultados_rapido.csv
Dashboard gravado em: ...\TAAL\resultados\resultados_rapido_dashboard.html

=== Resumo geral (todas as combinacoes planejadas) ===
Algoritmo               Registros   Sucessos Tempo medio(ms)
Backtracking                   50         30          0.870
BranchAndBound                 50         30          0.131
ProgramacaoDinamica            50         36         46.982
Guloso                         50         50          0.132
KarmarkarKarp                  50         50          0.158

=== Distribuicao dos status ===
Status                Registros
SUCESSO                     196
TEMPO_LIMITE                  0
MEMORIA_INVIAVEL             14
NAO_EXECUTADO                40
ERRO                          0

=== Qualidade da solucao (base comum a todos os algoritmos) ===
Base comparavel: 24 instancias com otimo comprovado em que os cinco algoritmos concluiram.
Algoritmo                Amostras     % Otimos   Desequilibrio%
Backtracking                   24       100.0%        3.253944
BranchAndBound                 24       100.0%        3.253944
ProgramacaoDinamica            24       100.0%        3.253944
Guloso                         24        58.3%        3.539037
KarmarkarKarp                  24        70.8%        3.365815
```

Um bom indício de que tudo está correto: os três algoritmos exatos exibem **exatamente o mesmo**
desequilíbrio médio, como deve acontecer se todos realmente encontram o ótimo. As heurísticas ficam
acima, nunca abaixo.

Duas observações importantes sobre essa saída, porque **não são erros**:

- Linhas com **`MEMORIA_INVIAVEL`** na Programação Dinâmica são um **resultado esperado e
  desejado** do experimento: demonstram o limite prático da abordagem pseudo-polinomial. É por isso
  que a coluna "Sucessos" da Programação Dinâmica é menor que "Registros".
- Linhas com **`NAO_EXECUTADO`** também não são erros. Na bateria padrão elas documentam o corte
  preventivo dos algoritmos exponenciais acima de 26 elementos. No modo de escalabilidade indicam
  que aquela combinação foi evitada após duas inviabilidades consecutivas já observadas.

Os números exatos de tempo variam conforme a máquina; as proporções entre algoritmos, não.

### Dashboard de métricas

Cada bateria também produz automaticamente um dashboard HTML ao lado do CSV. O nome é derivado da
saída escolhida: `resultados.csv` gera `resultados_dashboard.html`, e uma saída personalizada como
`saida/meu_teste.csv` gera `saida/meu_teste_dashboard.html`.

O dashboard oferece, em um único arquivo:

- filtros por perfil, algoritmo e status;
- indicadores de cobertura, sucesso, tempo, memória e limites observados;
- gráficos de tempo, memória e estados por tamanho em escala logarítmica;
- distribuição de status por algoritmo;
- comparação de qualidade e taxa de ótimo sobre a mesma base comum;
- tabela detalhada e paginada das execuções.

Todo o CSS, JavaScript e conjunto de dados fica embutido no HTML. Portanto, a página pode ser
copiada para outra máquina e aberta diretamente, sem instalar bibliotecas ou acessar a internet.

---

## Executando os testes automatizados

Os testes verificam corretude: preservação da soma total, concordância entre os três algoritmos
exatos, heurísticas nunca superando o ótimo, reprodutibilidade do gerador, geração segura do
dashboard autônomo e validação das configurações da interface sem precisar abrir uma janela.

**No VS Code:** clique no ícone de **Testing** (frasco de laboratório) na barra lateral e no botão
▶ **Run Tests**. Também é possível abrir `src/test/java/br/edu/taal/particao/PartitionAlgorithmsTest.java`
e clicar no ▶ ao lado de cada teste.

**No IntelliJ:** abra o mesmo arquivo e clique no ▶ ao lado da declaração da classe, escolhendo
**Run 'PartitionAlgorithmsTest'**.

**Por linha de comando (requer Maven):**

```bash
mvn test
```

> Os testes usam JUnit 5, cuja biblioteca é baixada pelo Maven. Por isso eles **não** rodam pelo
> caminho da [Opção C](#opção-c--linha-de-comando-sem-maven-mais-simples), que não baixa
> dependências. Isso não afeta a execução dos experimentos, que independe de bibliotecas externas.

---

## Solução de problemas

| Mensagem de erro | Causa | Solução |
|------------------|-------|---------|
| `javac não é reconhecido como um comando` / `javac: command not found` | JDK não instalado ou fora do PATH | Instale o JDK 17 conforme [esta seção](#como-instalar-o-jdk-17) e **reabra o terminal** |
| `java -version` funciona mas `javac -version` não | Só o JRE está instalado | Instale o **JDK** (não o JRE) |
| `A execução de scripts foi desabilitada neste sistema` | Política padrão do PowerShell | Use o comando completo com `powershell -ExecutionPolicy Bypass -File ...` como mostrado na Opção C |
| VS Code não mostra o botão **Run** sobre o `main` | Extensão de Java não instalada ou projeto ainda importando | Instale o *Extension Pack for Java* e aguarde a barra de progresso terminar |
| VS Code: `The declared package does not match the expected package` | A pasta aberta não é a raiz do projeto | Feche e abra novamente a pasta **`TAAL`**, a que contém o `pom.xml` |
| IntelliJ: `Cannot resolve symbol` em várias classes | Projeto Maven não foi importado | Clique com o botão direito no `pom.xml` > **Maven > Reload project** |
| IntelliJ: `Project SDK is not defined` | JDK não configurado no projeto | **File > Project Structure > Project > SDK** e selecione o JDK 17 |
| `UnsupportedClassVersionError` | JDK anterior ao 17 | Instale o JDK 17 ou superior e reconfigure a IDE |
| `mvn não é reconhecido` | Maven não instalado | Use a [Opção C](#opção-c--linha-de-comando-sem-maven-mais-simples), que dispensa o Maven |
| `OutOfMemoryError: Java heap space` | Pouca memória para a JVM | Rode com `-Xmx4g`. Se a máquina tiver pouca RAM, use `--rapido` |
| O programa parece travado | Bateria completa em andamento | É normal levar de 1 a 5 minutos. Use `--rapido` para verificar rapidamente |
| A interface informa que não há monitor | Execução em servidor ou terminal *headless* | Use os mesmos modos pela linha de comando; a interface exige um ambiente gráfico |
| `MEMORIA_INVIAVEL` na saída | **Não é erro** | É um resultado esperado do experimento, veja [esta observação](#o-que-você-deve-ver-ao-executar) |
| `WARNING: A restricted method in java.lang.System has been called` | **Não é erro** | Aviso emitido pelo agente do próprio IntelliJ (`idea_rt.jar`) em JDKs recentes. Não vem do código do projeto e não afeta os resultados |

Se nenhuma das opções acima resolver, o caminho mais confiável é o manual, que depende apenas do
JDK — execute a partir da pasta do projeto:

```bash
javac -encoding UTF-8 -d build/classes $(find src/main/java -name "*.java")
```

```bash
java -cp build/classes br.edu.taal.particao.Main --rapido
```

---

## Métricas coletadas

Para cada execução (`Metrics` e `ExecutionRecord`):

- tempo de execução (mediana, mínimo, máximo e desvio-padrão amostral, após *warm-up* da JVM);
- memória alocada no heap pela thread durante a execução;
- número de estados explorados;
- número de chamadas recursivas;
- número de podas realizadas;
- profundidade máxima da árvore de busca;
- diferença encontrada e diferença ótima de referência;
- três medidas de qualidade, detalhadas abaixo;
- status: `SUCESSO`, `TEMPO_LIMITE`, `MEMORIA_INVIAVEL`, `NAO_EXECUTADO` ou `ERRO`.

O *warm-up* existe porque a JVM compila o código sob demanda (JIT): as primeiras execuções de um
método são interpretadas e, portanto, muito mais lentas. Medir sem aquecer produziria tempos que
refletem o compilador, e não o algoritmo. O modo rápido, destinado apenas a conferir o ambiente,
usa 1 aquecimento e 3 medições. A bateria completa, usada na análise, emprega 2 aquecimentos e 7
medições. O modo de escalabilidade usa uma única medição, sem aquecimento por instância, porque seu
objetivo é testar se uma resolução cabe no limite de 5 segundos; após duas inviabilidades
consecutivas da mesma combinação algoritmo/perfil, os tamanhos seguintes são registrados como
`NAO_EXECUTADO`.

### Por que três medidas de qualidade

O GAP percentual sozinho é uma métrica instável neste problema, por duas razões:

1. **É indefinido quando o ótimo vale zero.** Como a partição perfeita é justamente o melhor
   resultado possível, instâncias com ótimo zero são frequentes — e nelas `(x − 0) / 0` não existe.
   Preencher esse caso com um valor fixo (por exemplo, 100%) faria uma solução de diferença 18 e
   outra de diferença 16 aparecerem como igualmente ruins.
2. **Explode quando o ótimo é pequeno.** Com ótimo 20 e solução 29.540, o GAP relativo é de
   147.600%, um número que diz pouco sobre a qualidade prática da partição.

Por isso o projeto registra três medidas complementares:

| Coluna | Definição | Quando usar |
|--------|-----------|-------------|
| `gap_absoluto` | `diferença − ótimo` | sempre definido; bom para instâncias com ótimo baixo |
| `gap_percentual` | `100·(diferença − ótimo)/ótimo` | comparação relativa; **vazio** quando o ótimo é zero |
| `desequilibrio_relativo_pct` | `100·diferença/somaTotal` | sempre definido e comparável entre instâncias de escalas diferentes |

### Ótimo comprovado versus melhor solução conhecida

Nas instâncias grandes nenhum algoritmo exato conclui, de modo que a referência passa a ser a melhor
solução encontrada pelas heurísticas. Nesses casos, "atingiu o ótimo" significaria apenas "igualou a
melhor heurística", o que inflaria artificialmente a taxa de acerto. A coluna
`referencia_comprovada` distingue os dois casos, e a coluna `atingiu_otimo` fica vazia quando não há
ótimo comprovado.

Pelo mesmo motivo, o resumo impresso ao final separa dois blocos: o desempenho considera apenas
execuções bem-sucedidas, mas a **qualidade** é reportada apenas sobre a base de instâncias em que os
cinco algoritmos concluíram. Médias calculadas sobre conjuntos diferentes de instâncias não são
comparáveis entre si — como os algoritmos exponenciais só rodam nas instâncias pequenas, uma
comparação ingênua chega a sugerir que uma heurística supera um algoritmo exato, o que é impossível.

## Instâncias de teste

`InstanceGenerator` produz seis perfis, todos reprodutíveis a partir de uma *seed*:

| Perfil | Característica | Por que existe |
|--------|----------------|----------------|
| `UNIFORME_PEQUENO` | valores em [1, 100] | muitas partições perfeitas existem |
| `UNIFORME_GRANDE` | valores em [1, 10⁶] | dificulta atingir diferença zero |
| `VALORES_ENORMES` | valores em [10⁶, 10⁸] | torna a tabela de PD inviável |
| `PARTICAO_PERFEITA` | diferença zero garantida por construção | mede corretude |
| `DOMINANTE` | um valor maior que a soma dos demais | caso clássico de dificuldade para o guloso |
| `SOMA_IMPAR` | valores pequenos com soma total ímpar | impede diferença zero e expõe o crescimento exponencial |

Os cinco perfis originais formam as baterias rápida e completa, preservando os conjuntos gerados
anteriormente. A completa combina tamanhos 10, 15, 20, 22, 24, 26, 100, 1.000 e 10.000 com cinco
variações, produzindo 1.125 registros — inclusive os casos `NAO_EXECUTADO`. `SOMA_IMPAR` é usado no
modo de escalabilidade, que testa tamanhos graduais de 10 até 50 com duas variações.

## Estrutura do projeto

```
TAAL/
├── pom.xml                            # configuração Maven
├── README.md
├── instancias/                        # PASTA PARA BATERIAS DE TESTE PRÓPRIAS
│   └── README.md                      # formato aceito (a pasta começa vazia)
├── exemplos_instancias/               # arquivos de exemplo prontos para testar
│   ├── exemplo_01_basico.txt
│   ├── exemplo_02_formato_livre.txt
│   └── exemplo_03_varias_instancias.txt
├── .vscode/                           # configurações de execução do VS Code
│   ├── extensions.json
│   ├── launch.json
│   └── settings.json
├── scripts/
│   ├── executar_sem_maven.ps1         # compila e roda usando apenas o JDK (Windows)
│   ├── executar_sem_maven.sh          # idem (Linux/macOS/Git Bash)
│   ├── executar_experimentos.ps1      # via Maven (Windows)
│   └── executar_experimentos.sh       # via Maven (Linux/macOS)
└── src/
    ├── main/java/br/edu/taal/particao/
    │   ├── Main.java                          # bateria de experimentos e relatório
    │   ├── ui/
    │   │   └── ExperimentGui.java             # interface Swing opcional e responsiva
    │   ├── model/
    │   │   ├── Instance.java                  # instância do problema
    │   │   ├── Metrics.java                   # métricas de uma execução
    │   │   └── PartitionResult.java           # resultado + cálculo de GAP
    │   ├── algorithms/
    │   │   ├── AbstractPartitionAlgorithm.java      # instrumentação uniforme de tempo e memória
    │   │   ├── PartitionAlgorithm.java        # contrato comum
    │   │   ├── BacktrackingPartition.java
    │   │   ├── BranchAndBoundPartition.java
    │   │   ├── DynamicProgrammingPartition.java
    │   │   ├── GreedyPartition.java
    │   │   ├── KarmarkarKarpPartition.java
    │   │   ├── TabelaInviavelException.java
    │   │   └── TempoLimiteExcedidoException.java
    │   └── experiment/
    │       ├── InstanceGenerator.java         # geração reprodutível de instâncias
    │       ├── InstanceFileReader.java        # leitura de baterias de teste externas
    │       ├── InstanceFormatException.java   # erro de formato com arquivo e linha
    │       ├── ExperimentRunner.java          # execução com warm-up e tempo limite
    │       ├── ScalabilityPolicy.java         # interrupção adaptativa por algoritmo/perfil
    │       ├── ExecutionRecord.java           # uma linha da tabela de resultados
    │       ├── CsvWriter.java                 # exportação para análise
    │       └── DashboardGenerator.java        # dashboard HTML autônomo e interativo
    └── test/java/br/edu/taal/particao/
        ├── PartitionAlgorithmsTest.java       # testes de corretude
        ├── DashboardGeneratorTest.java        # testes da exportação visual
        ├── experiment/InstanceFileReaderTest.java  # testes do leitor de instâncias externas
        └── ui/ExperimentGuiTest.java          # testes headless da configuração da interface
```

## Saídas em CSV e HTML

O arquivo gerado tem uma linha por (instância × algoritmo), com as colunas:

```
perfil, tamanho, instancia, algoritmo, exato, status, soma_a, soma_b, diferenca,
diferenca_referencia, referencia_comprovada, gap_absoluto, gap_percentual,
desequilibrio_relativo_pct, atingiu_otimo, tempo_ms, tempo_min_ms, tempo_max_ms,
tempo_desvio_padrao_ms, repeticoes_medicao, memoria_alocada_mb,
estados_explorados, chamadas_recursivas, podas, profundidade_maxima, observacao
```

A coluna `memoria_alocada_mb` registra o total de bytes alocados no heap pela thread durante a
resolução completa. Ela não representa apenas os objetos que permaneceram vivos ao final e, por
isso, não é anulada quando o coletor de lixo libera estruturas temporárias. Em uma JVM que não
ofereça o contador de alocações por thread, a coluna fica vazia em vez de registrar um zero
enganoso.

Para uma análise de qualidade estatisticamente válida, filtre por
`referencia_comprovada = true` — caso contrário estará comparando heurísticas com elas mesmas.

O separador decimal é o **ponto** e o separador de colunas é a **vírgula**. Ao abrir no Excel em
português, use **Dados > Obter Dados > De Texto/CSV** e selecione origem **UTF-8** para que os
números sejam interpretados corretamente.

## Reprodutibilidade

A mesma *seed* sempre gera exatamente as mesmas instâncias, em qualquer máquina. Para reproduzir os
resultados do relatório, use a seed padrão (`42`).

Para que os experimentos sejam reprodutíveis, o relatório registra: modelo do processador, memória
RAM, sistema operacional, versão do JDK, valor de `-Xmx` e a *seed* utilizada. As primeiras linhas
impressas pelo programa já reportam parte dessas informações automaticamente.

## Declaração de uso de IA

Conforme o normativo da disciplina, declaramos que ferramentas de IA generativa foram utilizadas
como apoio à organização do código, à redação de documentação e à estruturação do texto. As
decisões de projeto, a escolha dos algoritmos comparados, o desenho experimental e a análise crítica
dos resultados são de autoria da equipe.

*(Ajuste esta seção conforme o uso real feito pela equipe antes da entrega.)*
