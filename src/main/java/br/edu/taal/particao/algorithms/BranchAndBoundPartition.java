package br.edu.taal.particao.algorithms;

import br.edu.taal.particao.model.Instance;
import br.edu.taal.particao.model.Metrics;
import br.edu.taal.particao.model.PartitionResult;

import java.util.Arrays;

/**
 * Estrategia de Branch and Bound.
 *
 * <p>Assim como o Backtracking, explora a arvore de decisao (cada elemento
 * vai para o subconjunto A ou B), mas ordena os elementos em ordem
 * decrescente e usa uma funcao de bound para podar ramos que
 * comprovadamente nao podem melhorar a melhor solucao encontrada ate o
 * momento:</p>
 *
 * <pre>
 *   limiteInferior = max(0, |somaA - somaB| - somaRestante)
 * </pre>
 *
 * <p>onde {@code somaRestante} e a soma dos elementos ainda nao alocados.
 * Se {@code limiteInferior >= melhorDiferenca}, o ramo e podado, pois
 * mesmo no melhor cenario possivel (elementos restantes cancelando
 * perfeitamente o desequilibrio) o resultado nao supera a melhor solucao
 * atual.</p>
 *
 * <p>Complexidade de tempo no pior caso: O(2^n), porem tipicamente muito
 * menor na pratica devido as podas.</p>
 */
public class BranchAndBoundPartition extends AbstractPartitionAlgorithm {

    private int[] elementos;
    private Integer[] indicesOriginais; // mapeia posicao ordenada -> indice original da instancia
    private long[] somaSufixo; // somaSufixo[i] = soma de elementos[i..n-1]
    private Metrics metricas;
    private boolean[] melhorGrupo;
    private long melhorDiferenca;
    private boolean encontrouOtimoAbsoluto;

    @Override
    protected PartitionResult solveInternal(Instance instance) {
        int[] originais = instance.getElementos();
        int n = originais.length;

        // Ordenar decrescente melhora a eficacia da poda: decisoes com maior
        // impacto no desequilibrio sao tomadas primeiro. Mantemos o mapeamento
        // para os indices originais para reconstruir o grupo corretamente.
        this.indicesOriginais = new Integer[n];
        for (int i = 0; i < n; i++) {
            indicesOriginais[i] = i;
        }
        Arrays.sort(indicesOriginais, (a, b) -> Integer.compare(originais[b], originais[a]));

        this.elementos = new int[n];
        for (int i = 0; i < n; i++) {
            elementos[i] = originais[indicesOriginais[i]];
        }

        this.somaSufixo = new long[n + 1];
        for (int i = n - 1; i >= 0; i--) {
            somaSufixo[i] = somaSufixo[i + 1] + elementos[i];
        }

        this.metricas = new Metrics();
        this.melhorGrupo = new boolean[n];
        this.melhorDiferenca = Long.MAX_VALUE;
        this.encontrouOtimoAbsoluto = false;

        boolean[] grupoAtual = new boolean[n];
        if (n > 0) {
            grupoAtual[0] = true; // quebra de simetria
            branchAndBound(1, elementos[0], 0, grupoAtual);
        }

        boolean[] grupoOriginal = new boolean[n];
        long somaA = 0;
        long somaB = 0;
        for (int i = 0; i < n; i++) {
            grupoOriginal[indicesOriginais[i]] = melhorGrupo[i];
            if (melhorGrupo[i]) {
                somaA += elementos[i];
            } else {
                somaB += elementos[i];
            }
        }
        return new PartitionResult(getNome(), grupoOriginal, somaA, somaB, metricas);
    }

    private void branchAndBound(int indice, long somaA, long somaB, boolean[] grupoAtual) {
        metricas.incrementarChamadasRecursivas();
        metricas.incrementarEstadosExplorados();
        metricas.registrarProfundidade(indice);

        if (encontrouOtimoAbsoluto) {
            return;
        }

        if ((metricas.getChamadasRecursivas() & 0xFFFF) == 0 && Thread.currentThread().isInterrupted()) {
            throw new TempoLimiteExcedidoException("Branch and Bound interrompido por tempo limite.");
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

        long somaRestante = somaSufixo[indice];
        long limiteInferior = Math.abs(somaA - somaB) - somaRestante;
        if (limiteInferior < 0) {
            limiteInferior = 0;
        }
        if (limiteInferior >= melhorDiferenca) {
            metricas.incrementarPodas();
            return;
        }

        int valor = elementos[indice];

        grupoAtual[indice] = true;
        branchAndBound(indice + 1, somaA + valor, somaB, grupoAtual);
        if (encontrouOtimoAbsoluto) {
            return;
        }

        grupoAtual[indice] = false;
        branchAndBound(indice + 1, somaA, somaB + valor, grupoAtual);
    }

    @Override
    public String getNome() {
        return "BranchAndBound";
    }

    @Override
    public boolean isExato() {
        return true;
    }
}
