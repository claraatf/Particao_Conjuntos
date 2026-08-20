package br.edu.taal.particao.experiment;

import br.edu.taal.particao.model.Instance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifica a leitura de baterias de teste externas. Como esses arquivos sao
 * escritos a mao por quem avalia o trabalho, o leitor precisa aceitar variacoes
 * de formatacao e, quando recusar um arquivo, explicar exatamente o motivo.
 */
class InstanceFileReaderTest {

    private final InstanceFileReader leitor = new InstanceFileReader();

    private Path escrever(Path pasta, String nome, String conteudo) throws IOException {
        Path arquivo = pasta.resolve(nome);
        Files.writeString(arquivo, conteudo, StandardCharsets.UTF_8);
        return arquivo;
    }

    @Test
    void deveLerUmaInstanciaSimples(@TempDir Path pasta) throws IOException {
        Path arquivo = escrever(pasta, "caso.txt", "10\n20\n30\n");

        List<Instance> instancias = leitor.carregar(arquivo);

        assertEquals(1, instancias.size());
        assertEquals("caso", instancias.get(0).getNome());
        assertArrayEquals(new int[]{10, 20, 30}, instancias.get(0).getElementos());
    }

    @Test
    void deveAceitarSeparadoresMisturados(@TempDir Path pasta) throws IOException {
        Path arquivo = escrever(pasta, "livre.txt", "1, 2; 3\t4\n5   6\n");

        Instance instancia = leitor.carregar(arquivo).get(0);

        assertArrayEquals(new int[]{1, 2, 3, 4, 5, 6}, instancia.getElementos());
    }

    @Test
    void deveIgnorarComentariosELinhasEmBranco(@TempDir Path pasta) throws IOException {
        Path arquivo = escrever(pasta, "comentado.txt",
                "# cabecalho\n\n10 20 # dois valores\n\n# fim\n30\n");

        Instance instancia = leitor.carregar(arquivo).get(0);

        assertArrayEquals(new int[]{10, 20, 30}, instancia.getElementos());
    }

    @Test
    void deveUsarONomeDeclaradoNoComentario(@TempDir Path pasta) throws IOException {
        Path arquivo = escrever(pasta, "arquivo.txt", "# nome: caso_dificil\n5 5 5 5\n");

        assertEquals("caso_dificil", leitor.carregar(arquivo).get(0).getNome());
    }

    @Test
    void deveSepararVariasInstanciasNoMesmoArquivo(@TempDir Path pasta) throws IOException {
        Path arquivo = escrever(pasta, "bateria.txt",
                "# nome: a\n1 2 3\n---\n# nome: b\n4 5 6\n---\n7 8 9\n");

        List<Instance> instancias = leitor.carregar(arquivo);

        assertEquals(3, instancias.size());
        assertEquals("a", instancias.get(0).getNome());
        assertEquals("b", instancias.get(1).getNome());
        assertEquals("bateria_3", instancias.get(2).getNome(),
                "instancia sem nome explicito deve receber sufixo posicional");
        assertArrayEquals(new int[]{7, 8, 9}, instancias.get(2).getElementos());
    }

    @Test
    void separadorFinalNaoDeveCriarInstanciaVazia(@TempDir Path pasta) throws IOException {
        Path arquivo = escrever(pasta, "final.txt", "1 2 3\n---\n");

        assertEquals(1, leitor.carregar(arquivo).size());
    }

    @Test
    void deveLerTodosOsArquivosDeUmDiretorio(@TempDir Path pasta) throws IOException {
        escrever(pasta, "b.txt", "3 4\n");
        escrever(pasta, "a.txt", "1 2\n");
        Files.createDirectory(pasta.resolve("sub"));
        escrever(pasta.resolve("sub"), "c.txt", "5 6\n");
        escrever(pasta, "ignorado.md", "isto nao e uma instancia\n");

        List<Instance> instancias = leitor.carregar(pasta);

        assertEquals(3, instancias.size(), "deve ler subpastas e ignorar extensoes nao aceitas");
        assertEquals("a", instancias.get(0).getNome(), "a ordem deve ser alfabetica");
    }

    @Test
    void deveGarantirNomesUnicosEntreArquivos(@TempDir Path pasta) throws IOException {
        escrever(pasta, "caso.txt", "1 2\n");
        Files.createDirectory(pasta.resolve("sub"));
        escrever(pasta.resolve("sub"), "caso.txt", "3 4\n");

        List<Instance> instancias = leitor.carregar(pasta);

        assertEquals(2, instancias.size());
        assertEquals("caso", instancias.get(0).getNome());
        assertEquals("caso_2", instancias.get(1).getNome());
    }

    @Test
    void deveIgnorarMarcaDeOrdemDeByte(@TempDir Path pasta) throws IOException {
        // Editores do Windows gravam o BOM no inicio; sem trata-lo, o primeiro
        // numero deixaria de ser reconhecido.
        Path arquivo = escrever(pasta, "bom.txt", "﻿10 20\n");

        assertArrayEquals(new int[]{10, 20}, leitor.carregar(arquivo).get(0).getElementos());
    }

    @Test
    void deveApontarArquivoELinhaEmTextoInvalido(@TempDir Path pasta) throws IOException {
        Path arquivo = escrever(pasta, "ruim.txt", "10 20\nabc 30\n");

        InstanceFormatException erro = assertThrows(InstanceFormatException.class,
                () -> leitor.carregar(arquivo));

        assertTrue(erro.getMessage().contains("linha 2"), "a mensagem deve indicar a linha");
        assertTrue(erro.getMessage().contains("abc"), "a mensagem deve citar o trecho invalido");
    }

    @Test
    void deveRecusarValoresNegativos(@TempDir Path pasta) throws IOException {
        Path arquivo = escrever(pasta, "negativo.txt", "10 -5\n");

        InstanceFormatException erro = assertThrows(InstanceFormatException.class,
                () -> leitor.carregar(arquivo));

        assertTrue(erro.getMessage().contains("negativo"));
    }

    @Test
    void deveRecusarArquivoSemNumeros(@TempDir Path pasta) throws IOException {
        Path arquivo = escrever(pasta, "vazio.txt", "# apenas comentarios\n\n");

        InstanceFormatException erro = assertThrows(InstanceFormatException.class,
                () -> leitor.carregar(arquivo));

        assertTrue(erro.getMessage().contains("nao contem nenhum numero"));
    }

    @Test
    void deveRecusarCaminhoInexistente(@TempDir Path pasta) {
        InstanceFormatException erro = assertThrows(InstanceFormatException.class,
                () -> leitor.carregar(pasta.resolve("nao_existe")));

        assertTrue(erro.getMessage().contains("nao encontrado"));
    }

    @Test
    void deveRecusarDiretorioSemArquivosElegiveis(@TempDir Path pasta) throws IOException {
        escrever(pasta, "leiame.md", "sem instancias aqui\n");

        InstanceFormatException erro = assertThrows(InstanceFormatException.class,
                () -> leitor.carregar(pasta));

        assertTrue(erro.getMessage().contains("Nenhum arquivo de instancia"));
    }
}
