package br.edu.taal.particao.algorithms;

import br.edu.taal.particao.model.Instance;
import br.edu.taal.particao.model.Metrics;
import br.edu.taal.particao.model.PartitionResult;

/**
 * Estrategia de Programacao Dinamica (variante do Subset Sum).
 *
 * <p>Constroi uma tabela {@code alcancavel[i][s]} indicando se e possivel
 * obter a soma {@code s} usando algum subconjunto dos {@code i} primeiros
 * elementos. Ao final, procura-se a maior soma {@code s <= somaTotal/2}
 * alcancavel; a diferenca minima e {@code somaTotal - 2*s}.</p>
 *
 * <p>A tabela completa (e nao apenas uma linha) e mantida para permitir a
 * reconstrucao do subconjunto escolhido, e nao somente o valor otimo.</p>
 *
 * <p>Complexidade: O(n * somaTotal) de tempo e espaco. Note que esse custo
 * e <em>pseudo-polinomial</em>: cresce com o VALOR da soma dos elementos, e
 * nao apenas com a quantidade deles. Por isso instancias com valores
 * grandes tornam a abordagem inviavel em memoria, mesmo com poucos
 * elementos &mdash; comportamento que o experimento registra explicitamente
 * atraves de {@link TabelaInviavelException}.</p>
 */
public class DynamicProgrammingPartition implements PartitionAlgorithm {

    private final long limiteCelulas;

    /**
     * Usa como limite um quarto da memoria maxima da JVM (cada celula da
     * tabela ocupa 1 byte), deixando folga para o restante do experimento.
     */
    public DynamicProgrammingPartition() {
        this(Runtime.getRuntime().maxMemory() / 4);
    }

    public DynamicProgrammingPartition(long limiteCelulas) {
        this.limiteCelulas = limiteCelulas;
    }

    @Override
    public PartitionResult solve(Instance instance) {
        int[] elementos = instance.getElementos();
        int n = elementos.length;
        long somaTotal = instance.getSomaTotal();
        long metaLonga = somaTotal / 2;

        // A dimensao da tabela e indexada por int; somas acima de
        // Integer.MAX_VALUE ja sao inviaveis por definicao.
        if (metaLonga > Integer.MAX_VALUE - 1) {
            throw new TabelaInviavelException(String.format(
                    "Soma total %d exige tabela indexada acima do limite de int (n=%d).",
                    somaTotal, n));
        }

        int meta = (int) metaLonga;
        long celulas = (long) (n + 1) * (meta + 1);
        if (celulas > limiteCelulas) {
            throw new TabelaInviavelException(String.format(
                    "Tabela de PD exigiria %d celulas (n=%d, somaTotal=%d), acima do limite de %d.",
                    celulas, n, somaTotal, limiteCelulas));
        }

        Metrics metricas = new Metrics();
        long inicio = System.nanoTime();
        long memAntes = medirMemoriaUsada();

        boolean[][] alcancavel = new boolean[n + 1][meta + 1];
        alcancavel[0][0] = true;

        for (int i = 1; i <= n; i++) {
            if (Thread.currentThread().isInterrupted()) {
                throw new TempoLimiteExcedidoException("Programacao Dinamica interrompida por tempo limite.");
            }
            int valor = elementos[i - 1];
            for (int s = 0; s <= meta; s++) {
                metricas.incrementarEstadosExplorados();
                boolean semElemento = alcancavel[i - 1][s];
                boolean comElemento = s >= valor && alcancavel[i - 1][s - valor];
                alcancavel[i][s] = semElemento || comElemento;
            }
        }

        int melhorSoma = 0;
        for (int s = meta; s >= 0; s--) {
            if (alcancavel[n][s]) {
                melhorSoma = s;
                break;
            }
        }

        boolean[] grupo = new boolean[n];
        int somaRestante = melhorSoma;
        for (int i = n; i > 0; i--) {
            int valor = elementos[i - 1];
            // Se a soma nao era alcancavel sem este elemento, ele faz parte da solucao.
            if (!alcancavel[i - 1][somaRestante]) {
                grupo[i - 1] = true;
                somaRestante -= valor;
            }
        }

        long memDepois = medirMemoriaUsada();
        metricas.setTempoExecucaoNanos(System.nanoTime() - inicio);
        metricas.setMemoriaUsadaBytes(Math.max(0, memDepois - memAntes));
        metricas.registrarProfundidade(n);

        long somaA = melhorSoma;
        long somaB = somaTotal - melhorSoma;
        return new PartitionResult(getNome(), grupo, somaA, somaB, metricas);
    }

    private long medirMemoriaUsada() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    @Override
    public String getNome() {
        return "ProgramacaoDinamica";
    }

    @Override
    public boolean isExato() {
        return true;
    }
}
