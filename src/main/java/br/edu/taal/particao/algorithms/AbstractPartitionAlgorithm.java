package br.edu.taal.particao.algorithms;

import br.edu.taal.particao.model.Instance;
import br.edu.taal.particao.model.Metrics;
import br.edu.taal.particao.model.PartitionResult;

import java.lang.management.ManagementFactory;

/**
 * Base comum que mede, de forma uniforme, a execucao completa de cada
 * algoritmo. O cronometro envolve todo o processamento de
 * {@link #solveInternal(Instance)}, incluindo preparacao, ordenacao,
 * resolucao e reconstrucao da particao.
 *
 * <p>A memoria registrada corresponde ao total de bytes alocados no heap
 * pela thread durante a chamada. Essa medida nao depende de os objetos
 * continuarem vivos ao final e, portanto, nao sofre do problema da simples
 * diferenca de heap antes/depois, que frequentemente resultava em zero por
 * causa do coletor de lixo.</p>
 */
public abstract class AbstractPartitionAlgorithm implements PartitionAlgorithm {

    private static final long MEDICAO_INDISPONIVEL = -1L;
    private static final com.sun.management.ThreadMXBean THREAD_MX_BEAN = criarThreadMxBean();

    @Override
    public final PartitionResult solve(Instance instance) {
        long memoriaAntes = bytesAlocadosPelaThreadAtual();
        long inicio = System.nanoTime();

        PartitionResult resultado = solveInternal(instance);

        long tempoNanos = System.nanoTime() - inicio;
        long memoriaDepois = bytesAlocadosPelaThreadAtual();

        Metrics metricas = resultado.getMetricas();
        metricas.setTempoExecucaoNanos(tempoNanos);
        if (memoriaAntes >= 0 && memoriaDepois >= memoriaAntes) {
            metricas.setMemoriaAlocadaBytes(memoriaDepois - memoriaAntes);
        } else {
            metricas.setMemoriaAlocadaBytes(MEDICAO_INDISPONIVEL);
        }

        return resultado;
    }

    /** Implementacao especifica do algoritmo, instrumentada pelo metodo final {@link #solve(Instance)}. */
    protected abstract PartitionResult solveInternal(Instance instance);

    private static com.sun.management.ThreadMXBean criarThreadMxBean() {
        java.lang.management.ThreadMXBean beanPadrao = ManagementFactory.getThreadMXBean();
        if (!(beanPadrao instanceof com.sun.management.ThreadMXBean beanEstendido)
                || !beanEstendido.isThreadAllocatedMemorySupported()) {
            return null;
        }

        try {
            if (!beanEstendido.isThreadAllocatedMemoryEnabled()) {
                beanEstendido.setThreadAllocatedMemoryEnabled(true);
            }
            return beanEstendido.isThreadAllocatedMemoryEnabled() ? beanEstendido : null;
        } catch (SecurityException | UnsupportedOperationException e) {
            return null;
        }
    }

    private static long bytesAlocadosPelaThreadAtual() {
        if (THREAD_MX_BEAN == null) {
            return MEDICAO_INDISPONIVEL;
        }
        return THREAD_MX_BEAN.getThreadAllocatedBytes(Thread.currentThread().getId());
    }
}
