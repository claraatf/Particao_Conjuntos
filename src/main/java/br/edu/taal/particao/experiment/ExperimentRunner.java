package br.edu.taal.particao.experiment;

import br.edu.taal.particao.algorithms.PartitionAlgorithm;
import br.edu.taal.particao.algorithms.TabelaInviavelException;
import br.edu.taal.particao.algorithms.TempoLimiteExcedidoException;
import br.edu.taal.particao.model.Instance;
import br.edu.taal.particao.model.PartitionResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Executa um algoritmo sobre uma instancia sob um limite de tempo,
 * cuidando de aquecimento da JVM (warm-up) e da coleta de metricas.
 *
 * <p>O aquecimento existe porque a JVM compila codigo sob demanda (JIT): as
 * primeiras execucoes de um metodo sao interpretadas e, portanto, muito mais
 * lentas que as seguintes. Medir sem aquecer produziria tempos que refletem
 * o compilador, e nao o algoritmo.</p>
 */
public class ExperimentRunner {

    private final long tempoLimiteSegundos;
    private final int repeticoesAquecimento;
    private final int repeticoesMedicao;

    public ExperimentRunner(long tempoLimiteSegundos, int repeticoesAquecimento, int repeticoesMedicao) {
        this.tempoLimiteSegundos = tempoLimiteSegundos;
        this.repeticoesAquecimento = repeticoesAquecimento;
        this.repeticoesMedicao = repeticoesMedicao;
    }

    /**
     * Executa o algoritmo sobre a instancia. Retorna um registro de sucesso
     * com as medianas do tempo e dos bytes alocados nas repeticoes, ou um
     * registro de falha caso o algoritmo estoure o tempo limite ou nao
     * consiga alocar memoria.
     */
    public ExecutionRecord executar(PartitionAlgorithm algoritmo, Instance instancia, String perfil) {
        ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "exec-" + algoritmo.getNome());
            thread.setDaemon(true);
            return thread;
        });

        try {
            Future<PartitionResult> futuro = executor.submit(() -> {
                for (int i = 0; i < repeticoesAquecimento; i++) {
                    algoritmo.solve(instancia);
                }
                PartitionResult melhorMedicao = null;
                List<Long> tempos = new ArrayList<>();
                List<Long> memoriasAlocadas = new ArrayList<>();
                for (int i = 0; i < repeticoesMedicao; i++) {
                    PartitionResult atual = algoritmo.solve(instancia);
                    tempos.add(atual.getMetricas().getTempoExecucaoNanos());
                    if (atual.getMetricas().isMemoriaAlocadaDisponivel()) {
                        memoriasAlocadas.add(atual.getMetricas().getMemoriaAlocadaBytes());
                    }
                    melhorMedicao = atual;
                }
                if (melhorMedicao != null && !tempos.isEmpty()) {
                    tempos.sort(Long::compareTo);
                    melhorMedicao.getMetricas().setTempoExecucaoNanos(tempos.get(tempos.size() / 2));
                    if (!memoriasAlocadas.isEmpty()) {
                        memoriasAlocadas.sort(Long::compareTo);
                        melhorMedicao.getMetricas().setMemoriaAlocadaBytes(
                                memoriasAlocadas.get(memoriasAlocadas.size() / 2));
                    }
                }
                return melhorMedicao;
            });

            PartitionResult resultado = futuro.get(tempoLimiteSegundos, TimeUnit.SECONDS);
            return ExecutionRecord.sucesso(instancia, perfil, resultado, algoritmo.isExato());

        } catch (TimeoutException e) {
            return ExecutionRecord.falha(instancia, perfil, algoritmo.getNome(), algoritmo.isExato(),
                    ExecutionRecord.Status.TEMPO_LIMITE,
                    "Excedeu " + tempoLimiteSegundos + "s");

        } catch (ExecutionException e) {
            Throwable causa = e.getCause();
            if (causa instanceof TabelaInviavelException || causa instanceof OutOfMemoryError) {
                return ExecutionRecord.falha(instancia, perfil, algoritmo.getNome(), algoritmo.isExato(),
                        ExecutionRecord.Status.MEMORIA_INVIAVEL, causa.getMessage());
            }
            if (causa instanceof TempoLimiteExcedidoException) {
                return ExecutionRecord.falha(instancia, perfil, algoritmo.getNome(), algoritmo.isExato(),
                        ExecutionRecord.Status.TEMPO_LIMITE, causa.getMessage());
            }
            return ExecutionRecord.falha(instancia, perfil, algoritmo.getNome(), algoritmo.isExato(),
                    ExecutionRecord.Status.ERRO, String.valueOf(causa));

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ExecutionRecord.falha(instancia, perfil, algoritmo.getNome(), algoritmo.isExato(),
                    ExecutionRecord.Status.ERRO, "Execucao interrompida");

        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * Preenche, em cada registro do grupo, a diferenca otima de referencia:
     * a menor diferenca obtida por algum algoritmo exato que concluiu com
     * sucesso. Sem isso nao e possivel calcular o GAP das heuristicas.
     *
     * <p>Quando nenhum algoritmo exato conclui (instancias grandes), a melhor
     * solucao conhecida entre as heuristicas e usada como referencia, porem
     * marcada como <em>nao comprovada</em>. A distincao importa: nesses casos
     * "atingiu o otimo" significa apenas "igualou a melhor solucao conhecida",
     * e tratar os dois casos como equivalentes inflaria artificialmente a taxa
     * de acerto das heuristicas.</p>
     */
    public void definirReferenciaOtima(List<ExecutionRecord> registrosDaInstancia) {
        Long melhorExata = null;
        Long melhorQualquer = null;

        for (ExecutionRecord registro : registrosDaInstancia) {
            if (!registro.isSucesso()) {
                continue;
            }
            long diferenca = registro.getResultado().getDiferenca();
            if (melhorQualquer == null || diferenca < melhorQualquer) {
                melhorQualquer = diferenca;
            }
            if (registro.isExato() && (melhorExata == null || diferenca < melhorExata)) {
                melhorExata = diferenca;
            }
        }

        Long referencia = melhorExata != null ? melhorExata : melhorQualquer;
        boolean comprovada = melhorExata != null;
        for (ExecutionRecord registro : registrosDaInstancia) {
            registro.setDiferencaOtimaReferencia(referencia, comprovada);
        }
    }
}
