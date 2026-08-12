package br.edu.taal.particao;

import br.edu.taal.particao.algorithms.BacktrackingPartition;
import br.edu.taal.particao.algorithms.BranchAndBoundPartition;
import br.edu.taal.particao.algorithms.DynamicProgrammingPartition;
import br.edu.taal.particao.algorithms.GreedyPartition;
import br.edu.taal.particao.algorithms.KarmarkarKarpPartition;
import br.edu.taal.particao.algorithms.PartitionAlgorithm;
import br.edu.taal.particao.experiment.InstanceGenerator;
import br.edu.taal.particao.model.Instance;
import br.edu.taal.particao.model.PartitionResult;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PartitionAlgorithmsTest {

    private List<PartitionAlgorithm> todosAlgoritmos() {
        return Arrays.asList(
                new BacktrackingPartition(),
                new BranchAndBoundPartition(),
                new DynamicProgrammingPartition(),
                new GreedyPartition(),
                new KarmarkarKarpPartition());
    }

    private List<PartitionAlgorithm> algoritmosExatos() {
        return Arrays.asList(
                new BacktrackingPartition(),
                new BranchAndBoundPartition(),
                new DynamicProgrammingPartition());
    }

    @Test
    void particaoDevePreservarTodosOsElementos() {
        Instance instancia = new Instance("teste", new int[]{3, 1, 1, 2, 2, 1});
        long somaTotal = instancia.getSomaTotal();

        for (PartitionAlgorithm algoritmo : todosAlgoritmos()) {
            PartitionResult resultado = algoritmo.solve(instancia);
            assertEquals(somaTotal, resultado.getSomaA() + resultado.getSomaB(),
                    "Somas nao batem em " + algoritmo.getNome());

            long somaCalculada = 0;
            boolean[] grupo = resultado.getGrupo();
            for (int i = 0; i < instancia.getTamanho(); i++) {
                if (grupo[i]) {
                    somaCalculada += instancia.getElementos()[i];
                }
            }
            assertEquals(resultado.getSomaA(), somaCalculada,
                    "Grupo reportado nao corresponde a somaA em " + algoritmo.getNome());
        }
    }

    @Test
    void algoritmosExatosDevemEncontrarParticaoPerfeita() {
        Instance instancia = new Instance("perfeita", new int[]{10, 20, 15, 5, 25, 15});
        for (PartitionAlgorithm algoritmo : algoritmosExatos()) {
            PartitionResult resultado = algoritmo.solve(instancia);
            assertEquals(0, resultado.getDiferenca(),
                    algoritmo.getNome() + " deveria encontrar diferenca zero");
        }
    }

    @Test
    void algoritmosExatosDevemConcordarEmInstanciasAleatorias() {
        Random random = new Random(2026);
        for (int repeticao = 0; repeticao < 30; repeticao++) {
            int n = 4 + random.nextInt(11);
            int[] valores = new int[n];
            for (int i = 0; i < n; i++) {
                valores[i] = 1 + random.nextInt(200);
            }
            Instance instancia = new Instance("aleatoria_" + repeticao, valores);

            long referencia = -1;
            for (PartitionAlgorithm algoritmo : algoritmosExatos()) {
                long diferenca = algoritmo.solve(instancia).getDiferenca();
                if (referencia < 0) {
                    referencia = diferenca;
                } else {
                    assertEquals(referencia, diferenca,
                            algoritmo.getNome() + " divergiu na instancia " + Arrays.toString(valores));
                }
            }
        }
    }

    @Test
    void heuristicasNuncaDevemSuperarOOtimo() {
        Random random = new Random(7);
        PartitionAlgorithm exato = new DynamicProgrammingPartition();
        List<PartitionAlgorithm> heuristicas = Arrays.asList(
                new GreedyPartition(), new KarmarkarKarpPartition());

        for (int repeticao = 0; repeticao < 30; repeticao++) {
            int n = 5 + random.nextInt(16);
            int[] valores = new int[n];
            for (int i = 0; i < n; i++) {
                valores[i] = 1 + random.nextInt(500);
            }
            Instance instancia = new Instance("heuristica_" + repeticao, valores);
            long otimo = exato.solve(instancia).getDiferenca();

            for (PartitionAlgorithm heuristica : heuristicas) {
                long diferenca = heuristica.solve(instancia).getDiferenca();
                assertTrue(diferenca >= otimo,
                        heuristica.getNome() + " retornou valor melhor que o otimo, o que indica erro");
            }
        }
    }

    @Test
    void instanciaComParticaoPerfeitaDeveTerSomaPar() {
        InstanceGenerator gerador = new InstanceGenerator(42L);
        for (int variacao = 0; variacao < 5; variacao++) {
            Instance instancia = gerador.gerar(InstanceGenerator.Perfil.PARTICAO_PERFEITA, 20, variacao);
            assertEquals(20, instancia.getTamanho());
            assertEquals(0, instancia.getSomaTotal() % 2,
                    "Instancia de particao perfeita deve ter soma total par");
        }
    }

    @Test
    void geradorDeveSerReprodutivel() {
        InstanceGenerator primeiro = new InstanceGenerator(123L);
        InstanceGenerator segundo = new InstanceGenerator(123L);

        Instance a = primeiro.gerar(InstanceGenerator.Perfil.UNIFORME_GRANDE, 50, 3);
        Instance b = segundo.gerar(InstanceGenerator.Perfil.UNIFORME_GRANDE, 50, 3);

        assertTrue(Arrays.equals(a.getElementos(), b.getElementos()),
                "Mesma seed deveria produzir a mesma instancia");
    }

    @Test
    void instanciaVaziaNaoDeveQuebrarOsAlgoritmos() {
        Instance vazia = new Instance("vazia", new int[0]);
        for (PartitionAlgorithm algoritmo : todosAlgoritmos()) {
            PartitionResult resultado = algoritmo.solve(vazia);
            assertEquals(0, resultado.getDiferenca(), algoritmo.getNome() + " falhou em instancia vazia");
        }
    }
}
