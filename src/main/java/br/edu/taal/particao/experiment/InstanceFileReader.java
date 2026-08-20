package br.edu.taal.particao.experiment;

import br.edu.taal.particao.model.Instance;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Le instancias do problema a partir de arquivos de texto fornecidos por quem
 * executa os experimentos, permitindo submeter os cinco algoritmos a uma
 * bateria de testes externa sem alterar o codigo.
 *
 * <p><strong>Formato aceito</strong> (deliberadamente tolerante, para que
 * arquivos escritos a mao funcionem sem ajustes):</p>
 * <ul>
 *     <li>numeros inteiros nao negativos separados por espacos, tabulacoes,
 *     quebras de linha, virgulas ou ponto e virgula &mdash; a disposicao no
 *     arquivo e irrelevante;</li>
 *     <li>tudo que aparece depois de {@code #} e comentario;</li>
 *     <li>uma linha contendo apenas tres ou mais hifens ({@code ---}) separa
 *     varias instancias dentro de um mesmo arquivo;</li>
 *     <li>o comentario {@code # nome: minha_instancia} nomeia a instancia
 *     seguinte nos relatorios;</li>
 *     <li>linhas em branco sao ignoradas.</li>
 * </ul>
 *
 * <p>Quando um diretorio e informado, todos os arquivos com extensao
 * {@code .txt}, {@code .csv}, {@code .in} ou {@code .dat} sao lidos, incluindo
 * os de subpastas, em ordem alfabetica para garantir reprodutibilidade.</p>
 */
public class InstanceFileReader {

    /** Pasta lida por padrao quando nenhum caminho e informado. */
    public static final Path PASTA_PADRAO = Path.of("instancias");

    private static final Set<String> EXTENSOES_ACEITAS = Set.of("txt", "csv", "in", "dat");

    /** Marca de ordem de byte que editores do Windows inserem no inicio do arquivo. */
    private static final char BOM = '﻿';

    private static final String PREFIXO_NOME = "nome:";

    /**
     * Carrega as instancias de um arquivo ou de um diretorio.
     *
     * @throws InstanceFormatException se o caminho nao existir, se nao houver
     *         arquivos elegiveis ou se algum arquivo estiver malformado
     */
    public List<Instance> carregar(Path caminho) throws IOException {
        if (!Files.exists(caminho)) {
            throw new InstanceFormatException(
                    "Caminho de instancias nao encontrado: " + caminho.toAbsolutePath()
                            + System.lineSeparator()
                            + "Crie a pasta e coloque nela os arquivos .txt, ou informe o caminho "
                            + "com --instancias=<caminho>.");
        }

        List<Path> arquivos = Files.isDirectory(caminho)
                ? listarArquivosElegiveis(caminho)
                : List.of(caminho);

        if (arquivos.isEmpty()) {
            throw new InstanceFormatException(
                    "Nenhum arquivo de instancia encontrado em " + caminho.toAbsolutePath()
                            + System.lineSeparator()
                            + "Sao aceitos arquivos com extensao .txt, .csv, .in ou .dat.");
        }

        List<Instance> instancias = new ArrayList<>();
        Set<String> nomesUsados = new LinkedHashSet<>();
        for (Path arquivo : arquivos) {
            for (Instance instancia : lerArquivo(arquivo)) {
                instancias.add(renomearSeDuplicado(instancia, nomesUsados));
            }
        }
        return instancias;
    }

    /** Indica se a pasta padrao existe e contem ao menos um arquivo elegivel. */
    public boolean existemInstanciasNaPastaPadrao() {
        try {
            return Files.isDirectory(PASTA_PADRAO) && !listarArquivosElegiveis(PASTA_PADRAO).isEmpty();
        } catch (IOException e) {
            return false;
        }
    }

    private List<Path> listarArquivosElegiveis(Path diretorio) throws IOException {
        try (Stream<Path> caminhos = Files.walk(diretorio)) {
            return caminhos
                    .filter(Files::isRegularFile)
                    .filter(arquivo -> EXTENSOES_ACEITAS.contains(extensao(arquivo)))
                    .sorted(Comparator.comparing(Path::toString, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    private static String extensao(Path arquivo) {
        String nome = arquivo.getFileName().toString();
        int ponto = nome.lastIndexOf('.');
        return ponto < 0 ? "" : nome.substring(ponto + 1).toLowerCase(Locale.ROOT);
    }

    private List<Instance> lerArquivo(Path arquivo) throws IOException {
        List<String> linhas = lerLinhas(arquivo);
        List<Bloco> blocos = new ArrayList<>();
        Bloco atual = new Bloco();

        for (int i = 0; i < linhas.size(); i++) {
            String linha = linhas.get(i);
            if (i == 0 && !linha.isEmpty() && linha.charAt(0) == BOM) {
                linha = linha.substring(1);
            }
            int numeroLinha = i + 1;
            String semEspacos = linha.strip();

            if (semEspacos.startsWith("#")) {
                String diretiva = semEspacos.substring(1).strip();
                if (diretiva.toLowerCase(Locale.ROOT).startsWith(PREFIXO_NOME)) {
                    atual.nome = diretiva.substring(PREFIXO_NOME.length()).strip();
                }
                continue;
            }

            if (ehSeparador(semEspacos)) {
                if (!atual.estaVazio()) {
                    blocos.add(atual);
                }
                atual = new Bloco();
                continue;
            }

            int comentario = linha.indexOf('#');
            String conteudo = comentario >= 0 ? linha.substring(0, comentario) : linha;
            adicionarValores(conteudo, arquivo, numeroLinha, atual);
        }

        if (!atual.estaVazio()) {
            blocos.add(atual);
        }

        if (blocos.isEmpty()) {
            throw new InstanceFormatException(
                    "O arquivo " + arquivo.toAbsolutePath() + " nao contem nenhum numero."
                            + System.lineSeparator()
                            + "Cada instancia deve ter ao menos um inteiro nao negativo.");
        }

        return converterEmInstancias(blocos, arquivo);
    }

    /**
     * Le o arquivo como UTF-8 e, se ele tiver sido salvo na codificacao legada
     * do Windows, repete a leitura em ISO-8859-1. Como os valores sao numeros,
     * a codificacao so afeta comentarios acentuados, mas uma falha de leitura
     * interromperia a bateria inteira.
     */
    private List<String> lerLinhas(Path arquivo) throws IOException {
        try {
            return Files.readAllLines(arquivo, StandardCharsets.UTF_8);
        } catch (java.nio.charset.MalformedInputException e) {
            return Files.readAllLines(arquivo, StandardCharsets.ISO_8859_1);
        }
    }

    private static boolean ehSeparador(String linha) {
        return linha.length() >= 3 && linha.chars().allMatch(caractere -> caractere == '-');
    }

    private void adicionarValores(String conteudo, Path arquivo, int numeroLinha, Bloco bloco) {
        for (String token : conteudo.split("[\\s,;]+")) {
            if (token.isEmpty()) {
                continue;
            }
            long valor;
            try {
                valor = Long.parseLong(token);
            } catch (NumberFormatException e) {
                throw new InstanceFormatException(erro(arquivo, numeroLinha,
                        "\"" + token + "\" nao e um numero inteiro."
                                + " Use # para comentarios e separe os valores por espaco,"
                                + " virgula ou quebra de linha."));
            }
            if (valor < 0) {
                throw new InstanceFormatException(erro(arquivo, numeroLinha,
                        "valor negativo (" + valor + "). O problema da particao exige"
                                + " inteiros nao negativos."));
            }
            if (valor > Integer.MAX_VALUE) {
                throw new InstanceFormatException(erro(arquivo, numeroLinha,
                        "valor " + valor + " acima do maximo suportado ("
                                + Integer.MAX_VALUE + ")."));
            }
            bloco.valores.add((int) valor);
        }
    }

    private static String erro(Path arquivo, int numeroLinha, String detalhe) {
        return "Erro no arquivo " + arquivo.toAbsolutePath() + ", linha " + numeroLinha
                + ": " + detalhe;
    }

    private List<Instance> converterEmInstancias(List<Bloco> blocos, Path arquivo) {
        String base = nomeBase(arquivo);
        List<Instance> instancias = new ArrayList<>(blocos.size());
        for (int i = 0; i < blocos.size(); i++) {
            Bloco bloco = blocos.get(i);
            String nome;
            if (bloco.nome != null && !bloco.nome.isBlank()) {
                nome = bloco.nome;
            } else if (blocos.size() == 1) {
                nome = base;
            } else {
                nome = base + "_" + (i + 1);
            }
            int[] elementos = new int[bloco.valores.size()];
            for (int j = 0; j < elementos.length; j++) {
                elementos[j] = bloco.valores.get(j);
            }
            instancias.add(new Instance(nome, elementos));
        }
        return instancias;
    }

    private static String nomeBase(Path arquivo) {
        String nome = arquivo.getFileName().toString();
        int ponto = nome.lastIndexOf('.');
        return ponto < 0 ? nome : nome.substring(0, ponto);
    }

    /** Garante nomes unicos para que cada linha do CSV identifique uma instancia. */
    private Instance renomearSeDuplicado(Instance instancia, Set<String> nomesUsados) {
        String nome = instancia.getNome();
        if (nomesUsados.add(nome)) {
            return instancia;
        }
        int sufixo = 2;
        while (!nomesUsados.add(nome + "_" + sufixo)) {
            sufixo++;
        }
        return new Instance(nome + "_" + sufixo, instancia.getElementos());
    }

    /** Acumulador de um bloco de valores durante a leitura. */
    private static final class Bloco {
        private final List<Integer> valores = new ArrayList<>();
        private String nome;

        private boolean estaVazio() {
            return valores.isEmpty();
        }
    }
}
