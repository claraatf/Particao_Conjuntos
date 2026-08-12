package br.edu.taal.particao.model;

import java.util.Arrays;

/**
 * Representa uma instancia do Problema da Particao de Conjuntos:
 * um conjunto de numeros inteiros positivos a ser dividido em dois
 * subconjuntos cujas somas sejam iguais (ou tenham a menor diferenca possivel).
 */
public class Instance {

    private final String nome;
    private final int[] elementos;

    public Instance(String nome, int[] elementos) {
        this.nome = nome;
        this.elementos = elementos;
    }

    public String getNome() {
        return nome;
    }

    public int[] getElementos() {
        return elementos;
    }

    public int getTamanho() {
        return elementos.length;
    }

    public long getSomaTotal() {
        long soma = 0;
        for (int v : elementos) {
            soma += v;
        }
        return soma;
    }

    @Override
    public String toString() {
        return "Instance{nome='" + nome + "', tamanho=" + elementos.length +
                ", somaTotal=" + getSomaTotal() +
                ", elementos=" + (elementos.length <= 30 ? Arrays.toString(elementos) : "[...]") + '}';
    }
}
