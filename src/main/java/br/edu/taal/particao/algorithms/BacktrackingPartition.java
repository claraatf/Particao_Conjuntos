package br.edu.taal.particao.algorithms;

import br.edu.taal.particao.model.Instance;
import br.edu.taal.particao.model.Metrics;
import br.edu.taal.particao.model.PartitionResult;

/**
 * Estrategia de Backtracking (forca bruta com poda basica).
 *
 * <p>Explora exaustivamente a arvore binaria de decisao (cada elemento vai
 * para o subconjunto A ou B), com duas otimizacoes que ainda caracterizam
 * backtracking puro (sem uso de uma funcao de bound como em Branch and
 * Bound):</p>
 * <ul>
 *     <li>quebra de simetria: o primeiro elemento e sempre fixado no
 *     subconjunto A, pois trocar A por B produz a mesma particao;</li>
 *     <li>parada antecipada global assim que uma particao perfeita
 *     (diferenca zero) e encontrada, ja que essa e a melhor solucao
 *     possivel para o problema.</li>
 * </ul>
 *
 * <p>Complexidade de tempo no pior caso: O(2^n).</p>
 */
public class BacktrackingPartition implements PartitionAlgorithm {

    private int[] elementos;
    private Metrics metricas;
    private boolean[] melhorGrupo;
    private long melhorDiferenca;
    private boolean encontrouOtimoAbsoluto;

    @Override
    public PartitionResult solve(Instance instance) {
        this.elementos = instance.getElementos();
        this.metricas = new Metrics();
        this.melhorGrupo = new boolean[elementos.length];
        this.melhorDiferenca = Long.MAX_VALUE;
        this.encontrouOtimoAbsoluto = false;

        long inicio = System.nanoTime();
        long memAntes = medirMemoriaUsada();

        boolean[] grupoAtual = new boolean[elementos.length];
        if (elementos.length > 0) {
            grupoAtual[0] = true; // quebra de simetria
            backtrack(1, elementos[0], 0, grupoAtual);
        }

        long memDepois = medirMemoriaUsada();
        metricas.setTempoExecucaoNanos(System.nanoTime() - inicio);
        metricas.setMemoriaUsadaBytes(Math.max(0, memDepois - memAntes));

        long somaA = 0;
        long somaB = 0;
        for (int i = 0; i < elementos.length; i++) {
            if (melhorGrupo[i]) {
                somaA += elementos[i];
            } else {
                somaB += elementos[i];
            }
        }
        return new PartitionResult(getNome(), melhorGrupo.clone(), somaA, somaB, metricas);
    }

    private void backtrack(int indice, long somaA, long somaB, boolean[] grupoAtual) {
        metricas.incrementarChamadasRecursivas();
        metricas.incrementarEstadosExplorados();
        metricas.registrarProfundidade(indice);

        if (encontrouOtimoAbsoluto) {
            return;
        }

        if ((metricas.getChamadasRecursivas() & 0xFFFF) == 0 && Thread.currentThread().isInterrupted()) {
            throw new TempoLimiteExcedidoException("Backtracking interrompido por tempo limite.");
        }

        if (indice == elementos.length) {
            long diferenca = Math.abs(somaA - somaB);
            if (diferenca < melhorDiferenca) {
                melhorDiferenca = diferenca;
                System.arraycopy(grupoAtual, 0, melhorGrupo, 0, grupoAtual.length);
                if (diferenca == 0) {
                    encontrouOtimoAbsoluto = true;
                }
            }
            return;
        }

        int valor = elementos[indice];

        grupoAtual[indice] = true;
        backtrack(indice + 1, somaA + valor, somaB, grupoAtual);
        if (encontrouOtimoAbsoluto) {
            return;
        }

        grupoAtual[indice] = false;
        backtrack(indice + 1, somaA, somaB + valor, grupoAtual);
    }

    private long medirMemoriaUsada() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    @Override
    public String getNome() {
        return "Backtracking";
    }

    @Override
    public boolean isExato() {
        return true;
    }
}
