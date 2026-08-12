package br.edu.taal.particao.experiment;

import br.edu.taal.particao.model.Metrics;
import br.edu.taal.particao.model.PartitionResult;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Grava os registros de execucao em CSV para posterior analise em planilha
 * ou ferramenta estatistica. Usa ponto como separador decimal
 * ({@link Locale#US}) para evitar ambiguidade com o separador de colunas.
 */
public class CsvWriter {

    private static final String CABECALHO = String.join(",",
            "perfil",
            "tamanho",
            "instancia",
            "algoritmo",
            "exato",
            "status",
            "soma_a",
            "soma_b",
            "diferenca",
            "diferenca_referencia",
            "gap_percentual",
            "atingiu_otimo",
            "tempo_ms",
            "memoria_mb",
            "estados_explorados",
            "chamadas_recursivas",
            "podas",
            "profundidade_maxima",
            "observacao");

    public void escrever(Path arquivo, List<ExecutionRecord> registros) throws IOException {
        Path pai = arquivo.getParent();
        if (pai != null) {
            Files.createDirectories(pai);
        }

        try (PrintWriter writer = new PrintWriter(
                Files.newBufferedWriter(arquivo, StandardCharsets.UTF_8))) {
            writer.println(CABECALHO);
            for (ExecutionRecord registro : registros) {
                writer.println(formatarLinha(registro));
            }
        }
    }

    private String formatarLinha(ExecutionRecord registro) {
        StringBuilder linha = new StringBuilder();
        linha.append(escapar(registro.getPerfil())).append(',');
        linha.append(registro.getTamanho()).append(',');
        linha.append(escapar(registro.getNomeInstancia())).append(',');
        linha.append(escapar(registro.getNomeAlgoritmo())).append(',');
        linha.append(registro.isExato()).append(',');
        linha.append(registro.getStatus()).append(',');

        if (registro.isSucesso()) {
            PartitionResult resultado = registro.getResultado();
            Metrics metricas = resultado.getMetricas();
            linha.append(resultado.getSomaA()).append(',');
            linha.append(resultado.getSomaB()).append(',');
            linha.append(resultado.getDiferenca()).append(',');
            linha.append(valorOuVazio(registro.getDiferencaOtimaReferencia())).append(',');
            Double gap = registro.getGapPercentual();
            linha.append(gap == null ? "" : String.format(Locale.US, "%.4f", gap)).append(',');
            linha.append(valorOuVazio(registro.atingiuOtimo())).append(',');
            linha.append(String.format(Locale.US, "%.4f", metricas.getTempoExecucaoMillis())).append(',');
            linha.append(String.format(Locale.US, "%.4f", metricas.getMemoriaUsadaMB())).append(',');
            linha.append(metricas.getEstadosExplorados()).append(',');
            linha.append(metricas.getChamadasRecursivas()).append(',');
            linha.append(metricas.getPodasRealizadas()).append(',');
            linha.append(metricas.getProfundidadeMaxima()).append(',');
        } else {
            // 12 colunas de metricas ficam vazias quando a execucao nao concluiu.
            linha.append(",".repeat(12));
        }

        linha.append(escapar(registro.getObservacao()));
        return linha.toString();
    }

    private String valorOuVazio(Object valor) {
        return valor == null ? "" : valor.toString();
    }

    private String escapar(String texto) {
        if (texto == null) {
            return "";
        }
        if (texto.contains(",") || texto.contains("\"") || texto.contains("\n")) {
            return '"' + texto.replace("\"", "\"\"").replace("\n", " ") + '"';
        }
        return texto;
    }
}
