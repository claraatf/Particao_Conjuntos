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
import br.edu.taal.particao.model.Instance;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Ponto de entrada da bateria de experimentos.
 *
 * <p>Executa as cinco estrategias sobre instancias de diferentes perfis e
 * tamanhos, gravando todos os resultados em CSV e imprimindo um resumo no
 * console.</p>
 *
 * <p>Uso: {@code java -jar particao-conjuntos.jar [seed] [arquivoSaida]}</p>
 */
public class Main {

    /** Tamanhos usados com algoritmos exponenciais (Backtracking / Branch and Bound). */
    private static final int[] TAMANHOS_EXPONENCIAIS = {10, 15, 20, 22, 24, 26};

    /** Tamanhos usados com os algoritmos de tempo polinomial ou pseudo-polinomial. */
    private static final int[] TAMANHOS_ESCALAVEIS = {20, 100, 1_000, 10_000};

    private static final int VARIACOES_POR_CONFIGURACAO = 5;
    private static final long TEMPO_LIMITE_SEGUNDOS = 30;

    public static void main(String[] args) throws IOException {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 42L;
        Path arquivoSaida = args.length > 1
                ? Paths.get(args[1])
                : Paths.get("resultados", "resultados.csv");

        InstanceGenerator gerador = new InstanceGenerator(seed);
        ExperimentRunner runner = new ExperimentRunner(TEMPO_LIMITE_SEGUNDOS, 1, 3);

        List<PartitionAlgorithm> algoritmos = Arrays.asList(
                new BacktrackingPartition(),
                new BranchAndBoundPartition(),
                new DynamicProgrammingPartition(),
                new GreedyPartition(),
                new KarmarkarKarpPartition());

        System.out.println("=== Problema da Particao de Conjuntos - Estudo Comparativo ===");
        System.out.println("Seed: " + seed);
        System.out.println("Ambiente: Java " + System.getProperty("java.version")
                + " | " + System.getProperty("os.name")
                + " | processadores disponiveis: " + Runtime.getRuntime().availableProcessors()
                + " | memoria maxima da JVM: "
                + (Runtime.getRuntime().maxMemory() / (1024 * 1024)) + " MB");
        System.out.println();

        List<ExecutionRecord> todosRegistros = new ArrayList<>();

        for (InstanceGenerator.Perfil perfil : InstanceGenerator.Perfil.values()) {
            for (int tamanho : tamanhosParaPerfil()) {
                for (int variacao = 0; variacao < VARIACOES_POR_CONFIGURACAO; variacao++) {
                    Instance instancia = gerador.gerar(perfil, tamanho, variacao);
                    List<ExecutionRecord> registrosDaInstancia = new ArrayList<>();

                    for (PartitionAlgorithm algoritmo : algoritmos) {
                        if (!deveExecutar(algoritmo, tamanho)) {
                            continue;
                        }
                        ExecutionRecord registro = runner.executar(algoritmo, instancia, perfil.name());
                        registrosDaInstancia.add(registro);
                    }

                    runner.definirReferenciaOtima(registrosDaInstancia);
                    todosRegistros.addAll(registrosDaInstancia);
                    imprimirProgresso(instancia, registrosDaInstancia);
                }
            }
        }

        new CsvWriter().escrever(arquivoSaida, todosRegistros);
        System.out.println();
        System.out.println("Resultados gravados em: " + arquivoSaida.toAbsolutePath());
        imprimirResumo(todosRegistros);
    }

    /** Uniao dos tamanhos exponenciais e escalaveis, sem repeticoes, em ordem crescente. */
    private static int[] tamanhosParaPerfil() {
        return java.util.stream.IntStream
                .concat(Arrays.stream(TAMANHOS_EXPONENCIAIS), Arrays.stream(TAMANHOS_ESCALAVEIS))
                .distinct()
                .sorted()
                .toArray();
    }

    /**
     * Evita submeter algoritmos exponenciais a instancias grandes demais, o
     * que apenas consumiria o tempo limite sem gerar informacao nova. O corte
     * em 26 elementos corresponde a cerca de 2^26 estados no pior caso.
     */
    private static boolean deveExecutar(PartitionAlgorithm algoritmo, int tamanho) {
        boolean exponencial = algoritmo instanceof BacktrackingPartition
                || algoritmo instanceof BranchAndBoundPartition;
        if (exponencial) {
            return tamanho <= 26;
        }
        return true;
    }

    private static void imprimirProgresso(Instance instancia, List<ExecutionRecord> registros) {
        System.out.printf("Instancia %-32s (n=%5d, soma=%d)%n",
                instancia.getNome(), instancia.getTamanho(), instancia.getSomaTotal());
        for (ExecutionRecord registro : registros) {
            if (registro.isSucesso()) {
                Double gap = registro.getGapPercentual();
                System.out.printf(Locale.US,
                        "   %-22s diferenca=%-12d tempo=%9.3f ms  estados=%-12d gap=%s%n",
                        registro.getNomeAlgoritmo(),
                        registro.getResultado().getDiferenca(),
                        registro.getResultado().getMetricas().getTempoExecucaoMillis(),
                        registro.getResultado().getMetricas().getEstadosExplorados(),
                        gap == null ? "-" : String.format(Locale.US, "%.2f%%", gap));
            } else {
                System.out.printf("   %-22s %s (%s)%n",
                        registro.getNomeAlgoritmo(), registro.getStatus(), registro.getObservacao());
            }
        }
    }

    private static void imprimirResumo(List<ExecutionRecord> registros) {
        System.out.println();
        System.out.println("=== Resumo por algoritmo ===");
        System.out.printf("%-22s %10s %10s %12s %14s%n",
                "Algoritmo", "Execucoes", "Sucessos", "% Otimos", "Tempo medio(ms)");

        List<String> algoritmos = registros.stream()
                .map(ExecutionRecord::getNomeAlgoritmo)
                .distinct()
                .toList();

        for (String algoritmo : algoritmos) {
            List<ExecutionRecord> doAlgoritmo = registros.stream()
                    .filter(r -> r.getNomeAlgoritmo().equals(algoritmo))
                    .toList();
            long sucessos = doAlgoritmo.stream().filter(ExecutionRecord::isSucesso).count();
            long comReferencia = doAlgoritmo.stream()
                    .filter(r -> r.atingiuOtimo() != null).count();
            long otimos = doAlgoritmo.stream()
                    .filter(r -> Boolean.TRUE.equals(r.atingiuOtimo())).count();
            double tempoMedio = doAlgoritmo.stream()
                    .filter(ExecutionRecord::isSucesso)
                    .mapToDouble(r -> r.getResultado().getMetricas().getTempoExecucaoMillis())
                    .average().orElse(Double.NaN);

            System.out.printf(Locale.US, "%-22s %10d %10d %11s %14.3f%n",
                    algoritmo, doAlgoritmo.size(), sucessos,
                    comReferencia == 0 ? "-" : String.format(Locale.US, "%.1f%%",
                            100.0 * otimos / comReferencia),
                    tempoMedio);
        }
    }
}
