package br.edu.taal.particao.algorithms;

import br.edu.taal.particao.model.Instance;
import br.edu.taal.particao.model.Metrics;
import br.edu.taal.particao.model.PartitionResult;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Heuristica de Karmarkar-Karp, tambem chamada de "metodo das diferencas".
 *
 * <p>Repetidamente retira os dois maiores valores ainda disponiveis e os
 * substitui pela diferenca entre eles. Isso equivale a decidir que esses
 * dois elementos ficarao em subconjuntos <em>opostos</em>, sem ainda fixar
 * qual e qual. O ultimo valor restante e a diferenca final entre os dois
 * subconjuntos.</p>
 *
 * <p>Para reconstruir a particao, cada operacao registra uma aresta de
 * restricao "lados opostos" entre os dois elementos combinados. As arestas
 * formam uma arvore, que ao final e bicolorida por uma busca em largura:
 * cada cor corresponde a um dos subconjuntos.</p>
 *
 * <p>Complexidade: O(n log n), devido as operacoes de heap.</p>
 *
 * <p>Na pratica costuma produzir solucoes muito melhores que a estrategia
 * gulosa simples, embora tambem nao garanta otimalidade.</p>
 */
public class KarmarkarKarpPartition implements PartitionAlgorithm {

    @Override
    public PartitionResult solve(Instance instance) {
        int[] elementos = instance.getElementos();
        int n = elementos.length;

        Metrics metricas = new Metrics();
        long inicio = System.nanoTime();
        long memAntes = medirMemoriaUsada();

        // Cada no do heap e um par {valor, indiceRepresentante}.
        PriorityQueue<long[]> heap = new PriorityQueue<>(
                Math.max(1, n), Comparator.comparingLong((long[] no) -> no[0]).reversed());
        for (int i = 0; i < n; i++) {
            heap.add(new long[]{elementos[i], i});
        }

        List<List<Integer>> adjacencia = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            adjacencia.add(new ArrayList<>());
        }

        while (heap.size() > 1) {
            metricas.incrementarEstadosExplorados();
            long[] maior = heap.poll();
            long[] segundo = heap.poll();

            int idMaior = (int) maior[1];
            int idSegundo = (int) segundo[1];
            adjacencia.get(idMaior).add(idSegundo);
            adjacencia.get(idSegundo).add(idMaior);

            heap.add(new long[]{maior[0] - segundo[0], idMaior});
        }

        boolean[] grupo = new boolean[n];
        int[] cor = new int[n];
        java.util.Arrays.fill(cor, -1);

        int profundidadeMaxima = 0;
        for (int raiz = 0; raiz < n; raiz++) {
            if (cor[raiz] != -1) {
                continue;
            }
            cor[raiz] = 0;
            Deque<int[]> fila = new ArrayDeque<>();
            fila.add(new int[]{raiz, 0});
            while (!fila.isEmpty()) {
                int[] atual = fila.poll();
                int no = atual[0];
                int profundidade = atual[1];
                if (profundidade > profundidadeMaxima) {
                    profundidadeMaxima = profundidade;
                }
                for (int vizinho : adjacencia.get(no)) {
                    if (cor[vizinho] == -1) {
                        cor[vizinho] = 1 - cor[no];
                        fila.add(new int[]{vizinho, profundidade + 1});
                    }
                }
            }
        }

        long somaA = 0;
        long somaB = 0;
        for (int i = 0; i < n; i++) {
            if (cor[i] == 0) {
                grupo[i] = true;
                somaA += elementos[i];
            } else {
                somaB += elementos[i];
            }
        }

        long memDepois = medirMemoriaUsada();
        metricas.setTempoExecucaoNanos(System.nanoTime() - inicio);
        metricas.setMemoriaUsadaBytes(Math.max(0, memDepois - memAntes));
        metricas.registrarProfundidade(profundidadeMaxima);

        return new PartitionResult(getNome(), grupo, somaA, somaB, metricas);
    }

    private long medirMemoriaUsada() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    @Override
    public String getNome() {
        return "KarmarkarKarp";
    }

    @Override
    public boolean isExato() {
        return false;
    }
}
