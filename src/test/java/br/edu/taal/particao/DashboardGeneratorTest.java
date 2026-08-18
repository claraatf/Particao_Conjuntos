package br.edu.taal.particao;

import br.edu.taal.particao.algorithms.BacktrackingPartition;
import br.edu.taal.particao.algorithms.BranchAndBoundPartition;
import br.edu.taal.particao.algorithms.DynamicProgrammingPartition;
import br.edu.taal.particao.algorithms.GreedyPartition;
import br.edu.taal.particao.algorithms.KarmarkarKarpPartition;
import br.edu.taal.particao.algorithms.PartitionAlgorithm;
import br.edu.taal.particao.experiment.DashboardGenerator;
import br.edu.taal.particao.experiment.ExecutionRecord;
import br.edu.taal.particao.experiment.ExperimentRunner;
import br.edu.taal.particao.model.Instance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DashboardGeneratorTest {

    @Test
    void dashboardDeveSerAutonomoCompletoESeguro(@TempDir Path diretorio) throws IOException {
        Instance instancia = new Instance("instancia_teste", new int[]{19, 17, 13, 11, 7, 5});
        List<PartitionAlgorithm> algoritmos = List.of(
                new BacktrackingPartition(),
                new BranchAndBoundPartition(),
                new DynamicProgrammingPartition(),
                new GreedyPartition(),
                new KarmarkarKarpPartition());

        List<ExecutionRecord> registros = new ArrayList<>();
        for (PartitionAlgorithm algoritmo : algoritmos) {
            registros.add(ExecutionRecord.sucesso(
                    instancia, "TESTE", algoritmo.solve(instancia), algoritmo.isExato()));
        }
        new ExperimentRunner(5, 0, 1).definirReferenciaOtima(registros);

        Instance instanciaComTextoEspecial = new Instance("tentativa</script>", new int[]{1});
        for (ExecutionRecord.Status status : List.of(
                ExecutionRecord.Status.TEMPO_LIMITE,
                ExecutionRecord.Status.MEMORIA_INVIAVEL,
                ExecutionRecord.Status.NAO_EXECUTADO,
                ExecutionRecord.Status.ERRO)) {
            String observacao = status == ExecutionRecord.Status.ERRO
                    ? "falha </script><script>alert('x')</script>"
                    : "falha controlada " + status;
            registros.add(ExecutionRecord.falha(
                    instanciaComTextoEspecial, "TESTE", "Algoritmo" + status, false,
                    status, observacao));
        }

        Path csv = diretorio.resolve("resultado.teste.csv");
        Path dashboard = DashboardGenerator.caminhoDashboard(csv);
        assertEquals(diretorio.resolve("resultado.teste_dashboard.html"), dashboard);

        new DashboardGenerator().escrever(dashboard, registros, "TESTE", 42L);
        String html = Files.readString(dashboard, StandardCharsets.UTF_8);

        assertTrue(html.startsWith("<!doctype html>"));
        assertTrue(html.contains("const RAW_DATA"));
        assertTrue(html.contains("Backtracking"));
        assertTrue(html.contains("ProgramacaoDinamica"));
        assertTrue(html.contains("TEMPO_LIMITE"));
        assertTrue(html.contains("MEMORIA_INVIAVEL"));
        assertTrue(html.contains("NAO_EXECUTADO"));
        assertEquals(registros.size(), ocorrencias(html, "\"perfil\":"));
        assertFalse(html.contains("__DADOS__"));
        assertFalse(html.contains("<script src="));
        assertFalse(html.contains("http://"));
        assertFalse(html.contains("https://"));
        assertFalse(html.contains("NaN"));
        assertFalse(html.contains("Infinity"));
        assertFalse(html.contains("</script><script>alert"));
        assertTrue(html.contains("\\u003c/script\\u003e"));
    }

    private int ocorrencias(String texto, String trecho) {
        int quantidade = 0;
        int indice = 0;
        while ((indice = texto.indexOf(trecho, indice)) >= 0) {
            quantidade++;
            indice += trecho.length();
        }
        return quantidade;
    }
}
