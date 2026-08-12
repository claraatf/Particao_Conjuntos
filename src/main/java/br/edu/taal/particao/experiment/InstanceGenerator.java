package br.edu.taal.particao.experiment;

import br.edu.taal.particao.model.Instance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Gerador de instancias do problema. Todas as geracoes sao derivadas de uma
 * semente (seed) explicita, de modo que os experimentos sejam totalmente
 * reprodutiveis: a mesma semente sempre produz o mesmo conjunto de dados.
 *
 * <p>Diferentes perfis de instancia sao oferecidos porque a estrutura dos
 * valores &mdash; e nao apenas a quantidade deles &mdash; afeta fortemente o
 * comportamento de cada estrategia.</p>
 */
public class InstanceGenerator {

    /** Perfis de instancia com caracteristicas que estressam algoritmos diferentes. */
    public enum Perfil {
        /** Valores uniformes em faixa pequena: muitas particoes perfeitas existem. */
        UNIFORME_PEQUENO,
        /** Valores uniformes em faixa ampla: dificulta encontrar diferenca zero. */
        UNIFORME_GRANDE,
        /** Valores enormes: torna a tabela de Programacao Dinamica inviavel. */
        VALORES_ENORMES,
        /** Particao perfeita garantida por construcao: util para medir corretude. */
        PARTICAO_PERFEITA,
        /** Um valor dominante e varios pequenos: caso classico de falha do guloso. */
        DOMINANTE
    }

    private final long seed;

    public InstanceGenerator(long seed) {
        this.seed = seed;
    }

    public long getSeed() {
        return seed;
    }

    /**
     * Gera uma instancia com o perfil e tamanho informados. O parametro
     * {@code variacao} permite derivar multiplas instancias distintas de uma
     * mesma configuracao (ex.: 20 repeticoes por tamanho).
     */
    public Instance gerar(Perfil perfil, int tamanho, int variacao) {
        Random random = new Random(seed + perfil.ordinal() * 1_000_003L
                + tamanho * 31L + variacao * 7919L);
        String nome = String.format("%s_n%d_v%d", perfil.name().toLowerCase(), tamanho, variacao);

        switch (perfil) {
            case UNIFORME_PEQUENO:
                return new Instance(nome, valoresAleatorios(random, tamanho, 1, 100));
            case UNIFORME_GRANDE:
                return new Instance(nome, valoresAleatorios(random, tamanho, 1, 1_000_000));
            case VALORES_ENORMES:
                return new Instance(nome, valoresAleatorios(random, tamanho, 1_000_000, 100_000_000));
            case PARTICAO_PERFEITA:
                return new Instance(nome, particaoPerfeita(random, tamanho));
            case DOMINANTE:
                return new Instance(nome, comValorDominante(random, tamanho));
            default:
                throw new IllegalArgumentException("Perfil nao suportado: " + perfil);
        }
    }

    private int[] valoresAleatorios(Random random, int tamanho, int minimo, int maximo) {
        int[] valores = new int[tamanho];
        for (int i = 0; i < tamanho; i++) {
            valores[i] = minimo + random.nextInt(maximo - minimo + 1);
        }
        return valores;
    }

    /**
     * Constroi uma instancia cuja diferenca minima e comprovadamente zero:
     * gera metade dos valores livremente e espelha essa mesma multiplicidade
     * na outra metade, embaralhando o resultado.
     */
    private int[] particaoPerfeita(Random random, int tamanho) {
        List<Integer> valores = new ArrayList<>(tamanho);

        int paresRestantes = tamanho / 2;
        if (tamanho % 2 == 1 && tamanho >= 3) {
            // Para tamanho impar, um trio (a+b, a, b) mantem a particao perfeita
            // possivel: o maior valor sozinho equilibra os outros dois.
            int a = 1 + random.nextInt(500);
            int b = 1 + random.nextInt(500);
            valores.add(a + b);
            valores.add(a);
            valores.add(b);
            paresRestantes = (tamanho - 3) / 2;
        }

        for (int i = 0; i < paresRestantes; i++) {
            int v = 1 + random.nextInt(1000);
            valores.add(v);
            valores.add(v);
        }

        // Cobre os casos degenerados de tamanho 1 (particao perfeita impossivel).
        while (valores.size() < tamanho) {
            valores.add(1 + random.nextInt(1000));
        }

        Collections.shuffle(valores, random);

        int[] resultado = new int[valores.size()];
        for (int i = 0; i < valores.size(); i++) {
            resultado[i] = valores.get(i);
        }
        return resultado;
    }

    /**
     * Um unico elemento com valor maior que a soma de todos os outros. Nesse
     * caso a diferenca minima e forcosamente {@code dominante - soma(restantes)},
     * e estrategias gulosas tendem a se sair mal relativamente.
     */
    private int[] comValorDominante(Random random, int tamanho) {
        int[] valores = new int[tamanho];
        long somaPequenos = 0;
        for (int i = 1; i < tamanho; i++) {
            valores[i] = 1 + random.nextInt(100);
            somaPequenos += valores[i];
        }
        long dominante = somaPequenos + 1 + random.nextInt(500);
        valores[0] = (int) Math.min(dominante, Integer.MAX_VALUE);
        return valores;
    }
}
