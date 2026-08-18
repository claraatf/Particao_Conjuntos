package br.edu.taal.particao;

import br.edu.taal.particao.algorithms.BacktrackingPartition;
import br.edu.taal.particao.algorithms.BranchAndBoundPartition;
import br.edu.taal.particao.algorithms.DynamicProgrammingPartition;
import br.edu.taal.particao.algorithms.GreedyPartition;
import br.edu.taal.particao.algorithms.KarmarkarKarpPartition;
import br.edu.taal.particao.algorithms.PartitionAlgorithm;
import br.edu.taal.particao.experiment.CsvWriter;
import br.edu.taal.particao.experiment.ExecutionRecord;
import br.edu.taal.particao.experiment.ExperimentRunner;
import br.edu.taal.particao.experiment.InstanceGenerator;
import br.edu.taal.particao.experiment.ScalabilityPolicy;
import br.edu.taal.particao.model.Instance;

import java.io.IOException;
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
 * [--rapido | --escalabilidade]}</p>
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
        ESCALABILIDADE
    }

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
        Modo modo = Modo.COMPLETO;
        boolean modoExplicitamenteSelecionado = false;
        List<String> posicionais = new ArrayList<>();
        for (String argumento : args) {
            Modo modoSolicitado = null;
            if ("--rapido".equalsIgnoreCase(argumento)) {
                modoSolicitado = Modo.RAPIDO;
            } else if ("--escalabilidade".equalsIgnoreCase(argumento)) {
                modoSolicitado = Modo.ESCALABILIDADE;
            }

            if (modoSolicitado != null) {
                if (modoExplicitamenteSelecionado) {
                    throw new IllegalArgumentException(
                            "Use apenas um modo: --rapido ou --escalabilidade.");
                }
                modo = modoSolicitado;
                modoExplicitamenteSelecionado = true;
            } else {
                posicionais.add(argumento);
            }
        }

        if (posicionais.size() > 2) {
            throw new IllegalArgumentException(
                    "Uso: [seed] [arquivoSaida] [--rapido | --escalabilidade]");
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
        };
        int aquecimentos = switch (modo) {
            case RAPIDO -> AQUECIMENTOS_RAPIDO;
            case COMPLETO -> AQUECIMENTOS_COMPLETO;
            case ESCALABILIDADE -> AQUECIMENTOS_ESCALABILIDADE;
        };
        int medicoes = switch (modo) {
            case RAPIDO -> MEDICOES_RAPIDO;
            case COMPLETO -> MEDICOES_COMPLETO;
            case ESCALABILIDADE -> MEDICOES_ESCALABILIDADE;
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

        System.out.println("=== Problema da Particao de Conjuntos - Estudo Comparativo ===");
        System.out.println("Modo: " + descricaoModo(modo));
        System.out.println("Seed: " + seed);
        System.out.println("Repeticoes por execucao: " + aquecimentos
                + " aquecimento(s) + " + medicoes + " medicao(oes)");
        System.out.println("Tempo limite por combinacao: " + tempoLimite + "s");
        System.out.println("Ambiente: Java " + System.getProperty("java.version")
                + " | " + System.getProperty("os.name")
                + " | processadores disponiveis: " + Runtime.getRuntime().availableProcessors()
                + " | memoria maxima da JVM: "
                + (Runtime.getRuntime().maxMemory() / (1024 * 1024)) + " MB");
        System.out.println();

        long inicioTotal = System.nanoTime();
        List<ExecutionRecord> todosRegistros = new ArrayList<>();

        for (InstanceGenerator.Perfil perfil : perfis) {
            for (int tamanho : tamanhos) {
                for (int variacao = 0; variacao < variacoes; variacao++) {
                    Instance instancia = gerador.gerar(perfil, tamanho, variacao);
                    List<ExecutionRecord> registrosDaInstancia = new ArrayList<>();

                    for (PartitionAlgorithm algoritmo : algoritmos) {
                        ExecutionRecord registro;
                        if (modo == Modo.ESCALABILIDADE) {
                            String motivoBloqueio = politicaEscalabilidade.getMotivoBloqueio(
                                    perfil.name(), algoritmo);
                            if (motivoBloqueio != null) {
                                registro = ExecutionRecord.falha(
                                        instancia, perfil.name(), algoritmo.getNome(), algoritmo.isExato(),
                                        ExecutionRecord.Status.NAO_EXECUTADO, motivoBloqueio);
                            } else {
                                registro = runner.executar(algoritmo, instancia, perfil.name());
                                politicaEscalabilidade.registrar(registro);
                            }
                        } else if (!deveExecutarNaBateriaPadrao(algoritmo, tamanho)) {
                            registro = ExecutionRecord.falha(
                                    instancia, perfil.name(), algoritmo.getNome(), algoritmo.isExato(),
                                    ExecutionRecord.Status.NAO_EXECUTADO,
                                    "Corte de seguranca da bateria padrao: algoritmo exponencial limitado a n <= "
                                            + LIMITE_ALGORITMOS_EXPONENCIAIS + ".");
                        } else {
                            registro = runner.executar(algoritmo, instancia, perfil.name());
                        }
                        registrosDaInstancia.add(registro);
                    }

                    runner.definirReferenciaOtima(registrosDaInstancia);
                    todosRegistros.addAll(registrosDaInstancia);
                    imprimirProgresso(instancia, registrosDaInstancia);
                }
            }
        }

        new CsvWriter().escrever(arquivoSaida, todosRegistros);

        double duracaoSegundos = (System.nanoTime() - inicioTotal) / 1_000_000_000.0;
        System.out.println();
        System.out.printf(Locale.US, "Bateria concluida em %.1f segundos (%d registros).%n",
                duracaoSegundos, todosRegistros.size());
        System.out.println("Resultados gravados em: " + arquivoSaida.toAbsolutePath());
        imprimirResumo(todosRegistros);
    }

    /** Evita submeter algoritmos exponenciais a instancias grandes demais. */
    private static boolean deveExecutarNaBateriaPadrao(PartitionAlgorithm algoritmo, int tamanho) {
        boolean exponencial = algoritmo instanceof BacktrackingPartition
                || algoritmo instanceof BranchAndBoundPartition;
        if (exponencial) {
            return tamanho <= LIMITE_ALGORITMOS_EXPONENCIAIS;
        }
        return true;
    }

    private static int[] tamanhosDoModo(Modo modo) {
        return switch (modo) {
            case RAPIDO -> TAMANHOS_RAPIDO;
            case COMPLETO -> TAMANHOS_COMPLETO;
            case ESCALABILIDADE -> TAMANHOS_ESCALABILIDADE;
        };
    }

    private static String nomeArquivoPadrao(Modo modo) {
        return switch (modo) {
            case RAPIDO -> "resultados_rapido.csv";
            case COMPLETO -> "resultados.csv";
            case ESCALABILIDADE -> "resultados_escalabilidade.csv";
        };
    }

    private static String descricaoModo(Modo modo) {
        return switch (modo) {
            case RAPIDO -> "RAPIDO (bateria reduzida, cerca de 10 segundos)";
            case COMPLETO -> "COMPLETO (bateria integral, tipicamente 1 a 5 minutos)";
            case ESCALABILIDADE ->
                    "ESCALABILIDADE (tamanhos graduais e interrupcao adaptativa)";
        };
    }

    private static void imprimirProgresso(Instance instancia, List<ExecutionRecord> registros) {
        System.out.printf("Instancia %-32s (n=%5d, soma=%d)%n",
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
                System.out.printf(Locale.US,
                        "   %-22s diferenca=%-12d tempo=%9.3f ms  estados=%-12d %s%n",
                        registro.getNomeAlgoritmo(),
                        registro.getResultado().getDiferenca(),
                        registro.getResultado().getMetricas().getTempoExecucaoMillis(),
                        registro.getResultado().getMetricas().getEstadosExplorados(),
                        qualidade);
            } else {
                System.out.printf("   %-22s %s (%s)%n",
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
    private static void imprimirResumo(List<ExecutionRecord> registros) {
        List<String> algoritmos = registros.stream()
                .map(ExecutionRecord::getNomeAlgoritmo)
                .distinct()
                .toList();

        System.out.println();
        System.out.println("=== Resumo geral (todas as combinacoes planejadas) ===");
        System.out.printf("%-22s %10s %10s %14s%n",
                "Algoritmo", "Registros", "Sucessos", "Tempo medio(ms)");

        for (String algoritmo : algoritmos) {
            List<ExecutionRecord> doAlgoritmo = filtrarPorAlgoritmo(registros, algoritmo);
            long sucessos = doAlgoritmo.stream().filter(ExecutionRecord::isSucesso).count();
            double tempoMedio = doAlgoritmo.stream()
                    .filter(ExecutionRecord::isSucesso)
                    .mapToDouble(r -> r.getResultado().getMetricas().getTempoExecucaoMillis())
                    .average().orElse(Double.NaN);

            System.out.printf(Locale.US, "%-22s %10d %10d %14.3f%n",
                    algoritmo, doAlgoritmo.size(), sucessos, tempoMedio);
        }

        System.out.println();
        System.out.println("=== Distribuicao dos status ===");
        System.out.printf("%-20s %10s%n", "Status", "Registros");
        for (ExecutionRecord.Status status : ExecutionRecord.Status.values()) {
            long quantidade = registros.stream()
                    .filter(registro -> registro.getStatus() == status)
                    .count();
            System.out.printf("%-20s %10d%n", status, quantidade);
        }

        Set<String> baseComum = instanciasComunsATodos(registros, algoritmos);

        System.out.println();
        System.out.println("=== Qualidade da solucao (base comum a todos os algoritmos) ===");
        System.out.println("Base comparavel: " + baseComum.size()
                + " instancias com otimo comprovado em que os cinco algoritmos concluiram.");
        System.out.printf("%-22s %10s %12s %16s%n",
                "Algoritmo", "Amostras", "% Otimos", "Desequilibrio%");

        for (String algoritmo : algoritmos) {
            List<ExecutionRecord> comparaveis = filtrarPorAlgoritmo(registros, algoritmo).stream()
                    .filter(ExecutionRecord::isSucesso)
                    .filter(r -> baseComum.contains(r.getNomeInstancia()))
                    .toList();

            if (comparaveis.isEmpty()) {
                System.out.printf("%-22s %10d %12s %16s%n", algoritmo, 0, "-", "-");
                continue;
            }

            long otimos = comparaveis.stream()
                    .filter(r -> Boolean.TRUE.equals(r.atingiuOtimo())).count();
            double desequilibrioMedio = comparaveis.stream()
                    .mapToDouble(r -> r.getResultado().getDesequilibrioRelativo())
                    .average().orElse(Double.NaN);

            System.out.printf(Locale.US, "%-22s %10d %11.1f%% %15.6f%n",
                    algoritmo, comparaveis.size(),
                    100.0 * otimos / comparaveis.size(), desequilibrioMedio);
        }

        System.out.println();
        System.out.println("Nota: as instancias maiores ficam fora da base comum quando algum algoritmo");
        System.out.println("nao conclui ou e interrompido pelos limites. O CSV traz todas as combinacoes,");
        System.out.println("com referencia_comprovada indicando se o otimo e garantido.");
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
