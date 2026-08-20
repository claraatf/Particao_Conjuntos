package br.edu.taal.particao;

import br.edu.taal.particao.algorithms.BacktrackingPartition;
import br.edu.taal.particao.algorithms.BranchAndBoundPartition;
import br.edu.taal.particao.algorithms.DynamicProgrammingPartition;
import br.edu.taal.particao.algorithms.GreedyPartition;
import br.edu.taal.particao.algorithms.KarmarkarKarpPartition;
import br.edu.taal.particao.algorithms.PartitionAlgorithm;
import br.edu.taal.particao.experiment.CsvWriter;
import br.edu.taal.particao.experiment.DashboardGenerator;
import br.edu.taal.particao.experiment.ExecutionRecord;
import br.edu.taal.particao.experiment.ExperimentRunner;
import br.edu.taal.particao.experiment.InstanceFileReader;
import br.edu.taal.particao.experiment.InstanceFormatException;
import br.edu.taal.particao.experiment.InstanceGenerator;
import br.edu.taal.particao.experiment.ScalabilityPolicy;
import br.edu.taal.particao.model.Instance;
import br.edu.taal.particao.ui.ExperimentGui;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Ponto de entrada da bateria de experimentos.
 *
 * <p>Executa as cinco estrategias sobre instancias de diferentes perfis e
 * tamanhos, gravando todos os resultados em CSV e imprimindo um resumo no
 * console.</p>
 *
 * <p>Uso: {@code java -jar particao-conjuntos.jar [seed] [arquivoSaida]
 * [--rapido | --escalabilidade]} ou {@code java -jar
 * particao-conjuntos.jar --gui}.</p>
 *
 * <p>A opcao {@code --rapido} executa uma bateria reduzida (cerca de 10
 * segundos), util para verificar rapidamente que o ambiente esta
 * configurado. A opcao {@code --escalabilidade} procura empiricamente os
 * limites das estrategias. Sem elas, a bateria completa e executada.</p>
 */
public class Main {

    private enum Modo {
        RAPIDO,
        COMPLETO,
        ESCALABILIDADE,
        PERSONALIZADO
    }

    /** Prefixo da opcao que aponta para arquivos de instancias externos. */
    private static final String OPCAO_INSTANCIAS = "--instancias";

    /** Prefixo da opcao que ajusta o corte aplicado aos algoritmos exponenciais. */
    private static final String OPCAO_LIMITE_EXATOS = "--limite-exatos=";

    /** Perfil atribuido as instancias lidas de arquivos externos. */
    private static final String PERFIL_PERSONALIZADO = "PERSONALIZADA";

    /** Tamanhos da bateria completa. */
    private static final int[] TAMANHOS_COMPLETO = {10, 15, 20, 22, 24, 26, 100, 1_000, 10_000};

    /** Tamanhos da bateria reduzida (modo --rapido). */
    private static final int[] TAMANHOS_RAPIDO = {10, 15, 20, 100, 1_000};

    /** Tamanhos graduais usados para observar o limite das estrategias. */
    private static final int[] TAMANHOS_ESCALABILIDADE = {
            10, 15, 20, 22, 24, 26, 28, 30, 32, 35, 40, 50
    };

    /** Perfis originais, mantidos sem alteracao nas baterias rapida e completa. */
    private static final InstanceGenerator.Perfil[] PERFIS_PADRAO = {
            InstanceGenerator.Perfil.UNIFORME_PEQUENO,
            InstanceGenerator.Perfil.UNIFORME_GRANDE,
            InstanceGenerator.Perfil.VALORES_ENORMES,
            InstanceGenerator.Perfil.PARTICAO_PERFEITA,
            InstanceGenerator.Perfil.DOMINANTE
    };

    private static final int VARIACOES_COMPLETO = 5;
    private static final int VARIACOES_RAPIDO = 2;
    private static final int VARIACOES_ESCALABILIDADE = 2;

    private static final int AQUECIMENTOS_COMPLETO = 2;
    private static final int MEDICOES_COMPLETO = 7;
    private static final int AQUECIMENTOS_RAPIDO = 1;
    private static final int MEDICOES_RAPIDO = 3;

    // A bateria externa pode conter muitas instancias, entao usa-se um numero
    // moderado de repeticoes: suficiente para estabilizar o JIT sem multiplicar
    // por nove o tempo total de correcao.
    private static final int AQUECIMENTOS_PERSONALIZADO = 1;
    private static final int MEDICOES_PERSONALIZADO = 3;

    // O modo de escalabilidade mede uma unica execucao para que o timeout
    // represente o custo de resolver uma instancia, e nao o lote estatistico.
    private static final int AQUECIMENTOS_ESCALABILIDADE = 0;
    private static final int MEDICOES_ESCALABILIDADE = 1;

    private static final long TEMPO_LIMITE_SEGUNDOS = 30;
    private static final long TEMPO_LIMITE_ESCALABILIDADE_SEGUNDOS = 5;
    private static final int FALHAS_CONSECUTIVAS_ANTES_DE_INTERROMPER = 2;

    /**
     * Acima deste tamanho os algoritmos exponenciais nao sao executados: eles
     * apenas consumiriam o tempo limite sem gerar informacao nova. O corte
     * corresponde a cerca de 2^26 estados no pior caso.
     */
    private static final int LIMITE_ALGORITMOS_EXPONENCIAIS = 26;

    public static void main(String[] args) throws IOException {
        if (contemOpcaoGui(args)) {
            if (args.length != 1) {
                throw new IllegalArgumentException(
                        "Use --gui isoladamente; modo, seed e saida sao escolhidos na interface.");
            }
            ExperimentGui.abrir();
            return;
        }

        // Erros de uso e de formato sao previsiveis e devem ser apresentados de
        // forma legivel: um rastreamento de pilha faria um arquivo mal
        // formatado parecer uma falha do programa.
        try {
            executarExperimentos(args, System.out);
        } catch (InstanceFormatException | IllegalArgumentException e) {
            System.err.println();
            System.err.println("ERRO: " + e.getMessage());
            System.err.println();
            System.err.println("Consulte instancias/README.md para o formato aceito.");
            System.exit(1);
        }
    }

    /**
     * Executa a mesma bateria usada pela linha de comando, enviando o progresso
     * ao fluxo informado. A separacao permite que a interface Swing reutilize
     * integralmente a logica experimental sem redirecionar {@code System.out}.
     */
    public static void executarExperimentos(String[] args, PrintStream saida) throws IOException {
        if (saida == null) {
            throw new IllegalArgumentException("O fluxo de saida nao pode ser nulo.");
        }
        Modo modo = Modo.COMPLETO;
        boolean modoExplicitamenteSelecionado = false;
        Path caminhoInstancias = null;
        int limiteExatos = LIMITE_ALGORITMOS_EXPONENCIAIS;
        List<String> posicionais = new ArrayList<>();

        for (String argumento : args) {
            Modo modoSolicitado = null;
            if ("--rapido".equalsIgnoreCase(argumento)) {
                modoSolicitado = Modo.RAPIDO;
            } else if ("--escalabilidade".equalsIgnoreCase(argumento)) {
                modoSolicitado = Modo.ESCALABILIDADE;
            } else if (OPCAO_INSTANCIAS.equalsIgnoreCase(argumento)) {
                modoSolicitado = Modo.PERSONALIZADO;
                caminhoInstancias = InstanceFileReader.PASTA_PADRAO;
            } else if (argumento.toLowerCase(Locale.ROOT).startsWith(OPCAO_INSTANCIAS + "=")) {
                modoSolicitado = Modo.PERSONALIZADO;
                String valor = argumento.substring(OPCAO_INSTANCIAS.length() + 1).trim();
                if (valor.isEmpty()) {
                    throw new IllegalArgumentException(
                            "Informe o caminho: --instancias=<pasta ou arquivo>");
                }
                caminhoInstancias = Paths.get(valor);
            } else if (argumento.toLowerCase(Locale.ROOT).startsWith(OPCAO_LIMITE_EXATOS)) {
                limiteExatos = lerLimiteExatos(argumento);
                continue;
            }

            if (modoSolicitado != null) {
                if (modoExplicitamenteSelecionado) {
                    throw new IllegalArgumentException(
                            "Use apenas um modo: --rapido, --escalabilidade ou --instancias.");
                }
                modo = modoSolicitado;
                modoExplicitamenteSelecionado = true;
            } else {
                posicionais.add(argumento);
            }
        }

        if (posicionais.size() > 2) {
            throw new IllegalArgumentException(
                    "Uso: [seed] [arquivoSaida] [--rapido | --escalabilidade | --instancias[=<caminho>]]"
                            + " [--limite-exatos=<n>]");
        }

        long seed = !posicionais.isEmpty() ? Long.parseLong(posicionais.get(0)) : 42L;
        Path arquivoSaida = posicionais.size() > 1
                ? Paths.get(posicionais.get(1))
                : Paths.get("resultados", nomeArquivoPadrao(modo));

        int[] tamanhos = tamanhosDoModo(modo);
        InstanceGenerator.Perfil[] perfis = modo == Modo.ESCALABILIDADE
                ? InstanceGenerator.Perfil.values()
                : PERFIS_PADRAO;
        int variacoes = switch (modo) {
            case RAPIDO -> VARIACOES_RAPIDO;
            case COMPLETO -> VARIACOES_COMPLETO;
            case ESCALABILIDADE -> VARIACOES_ESCALABILIDADE;
            case PERSONALIZADO -> 0; // as instancias vem dos arquivos, nao do gerador
        };
        int aquecimentos = switch (modo) {
            case RAPIDO -> AQUECIMENTOS_RAPIDO;
            case COMPLETO -> AQUECIMENTOS_COMPLETO;
            case ESCALABILIDADE -> AQUECIMENTOS_ESCALABILIDADE;
            case PERSONALIZADO -> AQUECIMENTOS_PERSONALIZADO;
        };
        int medicoes = switch (modo) {
            case RAPIDO -> MEDICOES_RAPIDO;
            case COMPLETO -> MEDICOES_COMPLETO;
            case ESCALABILIDADE -> MEDICOES_ESCALABILIDADE;
            case PERSONALIZADO -> MEDICOES_PERSONALIZADO;
        };
        long tempoLimite = modo == Modo.ESCALABILIDADE
                ? TEMPO_LIMITE_ESCALABILIDADE_SEGUNDOS
                : TEMPO_LIMITE_SEGUNDOS;

        InstanceGenerator gerador = new InstanceGenerator(seed);
        ExperimentRunner runner = new ExperimentRunner(tempoLimite, aquecimentos, medicoes);
        ScalabilityPolicy politicaEscalabilidade = new ScalabilityPolicy(
                FALHAS_CONSECUTIVAS_ANTES_DE_INTERROMPER);

        List<PartitionAlgorithm> algoritmos = Arrays.asList(
                new BacktrackingPartition(),
                new BranchAndBoundPartition(),
                new DynamicProgrammingPartition(),
                new GreedyPartition(),
                new KarmarkarKarpPartition());

        // As instancias externas sao lidas antes de qualquer saida: um arquivo
        // malformado deve interromper a execucao imediatamente, com uma
        // mensagem clara, em vez de no meio do relatorio.
        List<Instance> instanciasPersonalizadas = modo == Modo.PERSONALIZADO
                ? new InstanceFileReader().carregar(caminhoInstancias)
                : List.of();

        saida.println("=== Problema da Particao de Conjuntos - Estudo Comparativo ===");
        saida.println("Modo: " + descricaoModo(modo));
        if (modo != Modo.PERSONALIZADO) {
            saida.println("Seed: " + seed);
        }
        saida.println("Repeticoes por execucao: " + aquecimentos
                + " aquecimento(s) + " + medicoes + " medicao(oes)");
        saida.println("Tempo limite por combinacao: " + tempoLimite + "s");
        saida.println("Ambiente: Java " + System.getProperty("java.version")
                + " | " + System.getProperty("os.name")
                + " | processadores disponiveis: " + Runtime.getRuntime().availableProcessors()
                + " | memoria maxima da JVM: "
                + (Runtime.getRuntime().maxMemory() / (1024 * 1024)) + " MB");

        // Sem este aviso, quem colocar arquivos na pasta e executar sem a opcao
        // veria apenas a bateria gerada, sem entender por que a sua foi ignorada.
        if (modo != Modo.PERSONALIZADO && new InstanceFileReader().existemInstanciasNaPastaPadrao()) {
            saida.println();
            saida.println("AVISO: ha arquivos de instancias em '" + InstanceFileReader.PASTA_PADRAO
                    + "', que NAO serao executados neste modo.");
            saida.println("       Para executa-los, use a opcao --instancias.");
        }
        saida.println();

        long inicioTotal = System.nanoTime();
        List<ExecutionRecord> todosRegistros = new ArrayList<>();

        if (modo == Modo.PERSONALIZADO) {
            saida.println("Instancias carregadas de " + caminhoInstancias.toAbsolutePath()
                    + ": " + instanciasPersonalizadas.size());
            saida.println();

            for (Instance instancia : instanciasPersonalizadas) {
                List<ExecutionRecord> registrosDaInstancia = executarAlgoritmos(
                        algoritmos, instancia, PERFIL_PERSONALIZADO, runner, limiteExatos);
                runner.definirReferenciaOtima(registrosDaInstancia);
                todosRegistros.addAll(registrosDaInstancia);
                imprimirProgresso(instancia, registrosDaInstancia, saida);
            }
        } else {
            for (InstanceGenerator.Perfil perfil : perfis) {
                for (int tamanho : tamanhos) {
                    for (int variacao = 0; variacao < variacoes; variacao++) {
                        Instance instancia = gerador.gerar(perfil, tamanho, variacao);
                        List<ExecutionRecord> registrosDaInstancia;

                        if (modo == Modo.ESCALABILIDADE) {
                            registrosDaInstancia = new ArrayList<>();
                            for (PartitionAlgorithm algoritmo : algoritmos) {
                                String motivoBloqueio = politicaEscalabilidade.getMotivoBloqueio(
                                        perfil.name(), algoritmo);
                                ExecutionRecord registro;
                                if (motivoBloqueio != null) {
                                    registro = ExecutionRecord.falha(
                                            instancia, perfil.name(), algoritmo.getNome(),
                                            algoritmo.isExato(),
                                            ExecutionRecord.Status.NAO_EXECUTADO, motivoBloqueio);
                                } else {
                                    registro = runner.executar(algoritmo, instancia, perfil.name());
                                    politicaEscalabilidade.registrar(registro);
                                }
                                registrosDaInstancia.add(registro);
                            }
                        } else {
                            registrosDaInstancia = executarAlgoritmos(
                                    algoritmos, instancia, perfil.name(), runner, limiteExatos);
                        }

                        runner.definirReferenciaOtima(registrosDaInstancia);
                        todosRegistros.addAll(registrosDaInstancia);
                        imprimirProgresso(instancia, registrosDaInstancia, saida);
                    }
                }
            }
        }

        new CsvWriter().escrever(arquivoSaida, todosRegistros);
        Path arquivoDashboard = DashboardGenerator.caminhoDashboard(arquivoSaida);
        new DashboardGenerator().escrever(
                arquivoDashboard, todosRegistros, descricaoModo(modo), seed);

        double duracaoSegundos = (System.nanoTime() - inicioTotal) / 1_000_000_000.0;
        saida.println();
        saida.printf(Locale.US, "Bateria concluida em %.1f segundos (%d registros).%n",
                duracaoSegundos, todosRegistros.size());
        saida.println("Resultados gravados em: " + arquivoSaida.toAbsolutePath());
        saida.println("Dashboard gravado em: " + arquivoDashboard.toAbsolutePath());
        imprimirResumo(todosRegistros, saida);
    }

    private static boolean contemOpcaoGui(String[] args) {
        return Arrays.stream(args).anyMatch("--gui"::equalsIgnoreCase);
    }

    /**
     * Executa os cinco algoritmos sobre uma instancia, aplicando o corte de
     * seguranca aos algoritmos exponenciais.
     */
    private static List<ExecutionRecord> executarAlgoritmos(List<PartitionAlgorithm> algoritmos,
                                                            Instance instancia, String perfil,
                                                            ExperimentRunner runner,
                                                            int limiteExatos) {
        List<ExecutionRecord> registros = new ArrayList<>(algoritmos.size());
        for (PartitionAlgorithm algoritmo : algoritmos) {
            if (ehExponencial(algoritmo) && instancia.getTamanho() > limiteExatos) {
                registros.add(ExecutionRecord.falha(
                        instancia, perfil, algoritmo.getNome(), algoritmo.isExato(),
                        ExecutionRecord.Status.NAO_EXECUTADO,
                        "Corte de seguranca: algoritmo exponencial limitado a n <= " + limiteExatos
                                + ". Use --limite-exatos=<n> para alterar."));
            } else {
                registros.add(runner.executar(algoritmo, instancia, perfil));
            }
        }
        return registros;
    }

    private static boolean ehExponencial(PartitionAlgorithm algoritmo) {
        return algoritmo instanceof BacktrackingPartition
                || algoritmo instanceof BranchAndBoundPartition;
    }

    private static int[] tamanhosDoModo(Modo modo) {
        return switch (modo) {
            case RAPIDO -> TAMANHOS_RAPIDO;
            case COMPLETO -> TAMANHOS_COMPLETO;
            case ESCALABILIDADE -> TAMANHOS_ESCALABILIDADE;
            case PERSONALIZADO -> new int[0];
        };
    }

    private static String nomeArquivoPadrao(Modo modo) {
        return switch (modo) {
            case RAPIDO -> "resultados_rapido.csv";
            case COMPLETO -> "resultados.csv";
            case ESCALABILIDADE -> "resultados_escalabilidade.csv";
            case PERSONALIZADO -> "resultados_personalizado.csv";
        };
    }

    private static String descricaoModo(Modo modo) {
        return switch (modo) {
            case RAPIDO -> "RAPIDO (bateria reduzida, cerca de 10 segundos)";
            case COMPLETO -> "COMPLETO (bateria integral, tipicamente 1 a 5 minutos)";
            case ESCALABILIDADE ->
                    "ESCALABILIDADE (tamanhos graduais e interrupcao adaptativa)";
            case PERSONALIZADO -> "PERSONALIZADO (instancias fornecidas em arquivos)";
        };
    }

    /** Interpreta {@code --limite-exatos=<n>}, validando o valor informado. */
    private static int lerLimiteExatos(String argumento) {
        String valor = argumento.substring(OPCAO_LIMITE_EXATOS.length()).trim();
        int limite;
        try {
            limite = Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Valor invalido em --limite-exatos: \"" + valor + "\". Informe um inteiro.");
        }
        if (limite < 0) {
            throw new IllegalArgumentException("--limite-exatos nao pode ser negativo.");
        }
        return limite;
    }

    private static void imprimirProgresso(Instance instancia, List<ExecutionRecord> registros,
                                          PrintStream saida) {
        saida.printf("Instancia %-32s (n=%5d, soma=%d)%n",
                instancia.getNome(), instancia.getTamanho(), instancia.getSomaTotal());
        for (ExecutionRecord registro : registros) {
            if (registro.isSucesso()) {
                Double gap = registro.getGapPercentual();
                Long gapAbsoluto = registro.getGapAbsoluto();
                // Quando o otimo e zero o GAP relativo e indefinido; nesse caso
                // exibe-se o GAP absoluto, que continua informativo.
                String qualidade;
                if (gap != null) {
                    qualidade = String.format(Locale.US, "gap=%.2f%%", gap);
                } else if (gapAbsoluto != null) {
                    qualidade = String.format(Locale.US, "gap=+%d (abs)", gapAbsoluto);
                } else {
                    qualidade = "gap=-";
                }
                saida.printf(Locale.US,
                        "   %-22s diferenca=%-12d tempo=%9.3f ms  estados=%-12d %s%n",
                        registro.getNomeAlgoritmo(),
                        registro.getResultado().getDiferenca(),
                        registro.getResultado().getMetricas().getTempoExecucaoMillis(),
                        registro.getResultado().getMetricas().getEstadosExplorados(),
                        qualidade);
            } else {
                saida.printf("   %-22s %s (%s)%n",
                        registro.getNomeAlgoritmo(), registro.getStatus(), registro.getObservacao());
            }
        }
    }

    /**
     * Imprime dois blocos distintos. O primeiro cobre todas as combinacoes de
     * cada algoritmo. O segundo restringe-se as instancias em que existe otimo
     * comprovado (ou seja, em que algum algoritmo exato concluiu), unico
     * recorte em que as medidas de qualidade sao comparaveis entre linhas:
     * como os algoritmos exponenciais podem nao concluir nas instancias grandes,
     * comparar medias calculadas sobre conjuntos diferentes de instancias
     * levaria a conclusoes invertidas.
     */
    private static void imprimirResumo(List<ExecutionRecord> registros, PrintStream saida) {
        List<String> algoritmos = registros.stream()
                .map(ExecutionRecord::getNomeAlgoritmo)
                .distinct()
                .toList();

        saida.println();
        saida.println("=== Resumo geral (todas as combinacoes planejadas) ===");
        saida.printf("%-22s %10s %10s %14s%n",
                "Algoritmo", "Registros", "Sucessos", "Tempo medio(ms)");

        for (String algoritmo : algoritmos) {
            List<ExecutionRecord> doAlgoritmo = filtrarPorAlgoritmo(registros, algoritmo);
            long sucessos = doAlgoritmo.stream().filter(ExecutionRecord::isSucesso).count();
            double tempoMedio = doAlgoritmo.stream()
                    .filter(ExecutionRecord::isSucesso)
                    .mapToDouble(r -> r.getResultado().getMetricas().getTempoExecucaoMillis())
                    .average().orElse(Double.NaN);

            saida.printf(Locale.US, "%-22s %10d %10d %14.3f%n",
                    algoritmo, doAlgoritmo.size(), sucessos, tempoMedio);
        }

        saida.println();
        saida.println("=== Distribuicao dos status ===");
        saida.printf("%-20s %10s%n", "Status", "Registros");
        for (ExecutionRecord.Status status : ExecutionRecord.Status.values()) {
            long quantidade = registros.stream()
                    .filter(registro -> registro.getStatus() == status)
                    .count();
            saida.printf("%-20s %10d%n", status, quantidade);
        }

        Set<String> baseComum = instanciasComunsATodos(registros, algoritmos);

        saida.println();
        saida.println("=== Qualidade da solucao (base comum a todos os algoritmos) ===");
        saida.println("Base comparavel: " + baseComum.size()
                + " instancias com otimo comprovado em que os cinco algoritmos concluiram.");
        saida.printf("%-22s %10s %12s %16s%n",
                "Algoritmo", "Amostras", "% Otimos", "Desequilibrio%");

        for (String algoritmo : algoritmos) {
            List<ExecutionRecord> comparaveis = filtrarPorAlgoritmo(registros, algoritmo).stream()
                    .filter(ExecutionRecord::isSucesso)
                    .filter(r -> baseComum.contains(r.getNomeInstancia()))
                    .toList();

            if (comparaveis.isEmpty()) {
                saida.printf("%-22s %10d %12s %16s%n", algoritmo, 0, "-", "-");
                continue;
            }

            long otimos = comparaveis.stream()
                    .filter(r -> Boolean.TRUE.equals(r.atingiuOtimo())).count();
            double desequilibrioMedio = comparaveis.stream()
                    .mapToDouble(r -> r.getResultado().getDesequilibrioRelativo())
                    .average().orElse(Double.NaN);

            saida.printf(Locale.US, "%-22s %10d %11.1f%% %15.6f%n",
                    algoritmo, comparaveis.size(),
                    100.0 * otimos / comparaveis.size(), desequilibrioMedio);
        }

        saida.println();
        saida.println("Nota: as instancias maiores ficam fora da base comum quando algum algoritmo");
        saida.println("nao conclui ou e interrompido pelos limites. O CSV traz todas as combinacoes,");
        saida.println("com referencia_comprovada indicando se o otimo e garantido.");
    }

    /**
     * Instancias em que <em>todos</em> os algoritmos concluiram com sucesso e
     * existe otimo comprovado. Medias de qualidade calculadas sobre bases
     * diferentes nao sao comparaveis entre si: como os algoritmos exponenciais
     * so rodam nas instancias pequenas, compara-los com heuristicas que tambem
     * rodaram nas grandes pode sugerir, incorretamente, que uma heuristica
     * supera um algoritmo exato.
     */
    private static Set<String> instanciasComunsATodos(List<ExecutionRecord> registros,
                                                      List<String> algoritmos) {
        Map<String, Set<String>> algoritmosPorInstancia = new LinkedHashMap<>();
        Set<String> comOtimoComprovado = new HashSet<>();

        for (ExecutionRecord registro : registros) {
            if (!registro.isSucesso()) {
                continue;
            }
            algoritmosPorInstancia
                    .computeIfAbsent(registro.getNomeInstancia(), chave -> new HashSet<>())
                    .add(registro.getNomeAlgoritmo());
            if (registro.isReferenciaComprovada()) {
                comOtimoComprovado.add(registro.getNomeInstancia());
            }
        }

        Set<String> base = new LinkedHashSet<>();
        for (Map.Entry<String, Set<String>> entrada : algoritmosPorInstancia.entrySet()) {
            if (entrada.getValue().size() == algoritmos.size()
                    && comOtimoComprovado.contains(entrada.getKey())) {
                base.add(entrada.getKey());
            }
        }
        return base;
    }

    private static List<ExecutionRecord> filtrarPorAlgoritmo(List<ExecutionRecord> registros, String algoritmo) {
        return registros.stream()
                .filter(r -> r.getNomeAlgoritmo().equals(algoritmo))
                .toList();
    }
}
