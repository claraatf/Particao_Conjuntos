package br.edu.taal.particao.algorithms;

import br.edu.taal.particao.model.Instance;
import br.edu.taal.particao.model.Metrics;
import br.edu.taal.particao.model.PartitionResult;

import java.util.Arrays;

/**
 * Estrategia Gulosa (Longest Processing Time first).
 *
 * <p>Ordena os elementos em ordem decrescente e, a cada passo, coloca o
 * proximo elemento no subconjunto que atualmente possui a menor soma. A
 * decisao e local e nunca revista, o que torna o algoritmo muito rapido
 * porem sem garantia de otimalidade.</p>
 *
 * <p>Complexidade: O(n log n), dominada pela ordenacao.</p>
 */
public class GreedyPartition extends AbstractPartitionAlgorithm {

    @Override
    protected PartitionResult solveInternal(Instance instance) {
        int[] originais = instance.getElementos();
        int n = originais.length;

        Metrics metricas = new Metrics();
        Integer[] indices = new Integer[n];
        for (int i = 0; i < n; i++) {
            indices[i] = i;
        }
        Arrays.sort(indices, (a, b) -> Integer.compare(originais[b], originais[a]));

        boolean[] grupo = new boolean[n];
        long somaA = 0;
        long somaB = 0;

        for (int k = 0; k < n; k++) {
            metricas.incrementarEstadosExplorados();
            int indice = indices[k];
            int valor = originais[indice];
            if (somaA <= somaB) {
                grupo[indice] = true;
                somaA += valor;
            } else {
                grupo[indice] = false;
                somaB += valor;
            }
        }

        metricas.registrarProfundidade(n);

        return new PartitionResult(getNome(), grupo, somaA, somaB, metricas);
    }

    @Override
    public String getNome() {
        return "Guloso";
    }

    @Override
    public boolean isExato() {
        return false;
    }
}
