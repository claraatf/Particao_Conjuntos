package br.edu.taal.particao.algorithms;

import br.edu.taal.particao.model.Instance;
import br.edu.taal.particao.model.PartitionResult;

/**
 * Contrato comum a todas as estrategias de resolucao do Problema da
 * Particao de Conjuntos, permitindo que o executor de experimentos
 * trate cada algoritmo de forma uniforme.
 */
public interface PartitionAlgorithm {

    /**
     * Resolve a instancia informada, dividindo os elementos em dois
     * subconjuntos e retornando o resultado com as metricas coletadas.
     */
    PartitionResult solve(Instance instance);

    /** Nome legivel do algoritmo, usado em relatorios e no CSV de resultados. */
    String getNome();

    /**
     * Indica se o algoritmo garante encontrar a solucao otima (exato) ou
     * se e uma aproximacao/heuristica.
     */
    boolean isExato();
}
