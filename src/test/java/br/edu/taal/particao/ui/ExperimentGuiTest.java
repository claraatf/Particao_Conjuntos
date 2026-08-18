package br.edu.taal.particao.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExperimentGuiTest {

    @Test
    void deveMontarArgumentosDosTresModosSemPerderCaminhoComEspacos(@TempDir Path diretorio) {
        Path semExtensao = diretorio.resolve("pasta com espacos").resolve("meu resultado");

        String[] rapido = ExperimentGui.criarArgumentos(
                ExperimentGui.ModoInterface.RAPIDO, " 7 ", semExtensao.toString());
        assertEquals(3, rapido.length);
        assertEquals("7", rapido[0]);
        assertEquals(semExtensao.toAbsolutePath() + ".csv", rapido[1]);
        assertEquals("--rapido", rapido[2]);

        String[] completo = ExperimentGui.criarArgumentos(
                ExperimentGui.ModoInterface.COMPLETO, "42", semExtensao + ".csv");
        assertEquals(2, completo.length);
        assertEquals("42", completo[0]);
        assertEquals(semExtensao.toAbsolutePath() + ".csv", completo[1]);

        String[] escalabilidade = ExperimentGui.criarArgumentos(
                ExperimentGui.ModoInterface.ESCALABILIDADE, "-19", semExtensao.toString());
        assertEquals("--escalabilidade", escalabilidade[2]);
    }

    @Test
    void deveValidarSeedECaminhoAntesDeIniciar() {
        assertThrows(IllegalArgumentException.class, () -> ExperimentGui.criarArgumentos(
                ExperimentGui.ModoInterface.RAPIDO, "sete", "resultado.csv"));
        assertThrows(IllegalArgumentException.class, () -> ExperimentGui.criarArgumentos(
                ExperimentGui.ModoInterface.RAPIDO, "42", "   "));
        assertThrows(IllegalArgumentException.class, () -> ExperimentGui.criarArgumentos(
                null, "42", "resultado.csv"));
    }

    @Test
    void caminhosPadraoDevemSerAbsolutosEEspecificosPorModo() {
        Path rapido = ExperimentGui.caminhoPadrao(ExperimentGui.ModoInterface.RAPIDO);
        Path completo = ExperimentGui.caminhoPadrao(ExperimentGui.ModoInterface.COMPLETO);
        Path escalabilidade = ExperimentGui.caminhoPadrao(
                ExperimentGui.ModoInterface.ESCALABILIDADE);

        assertTrue(rapido.isAbsolute());
        assertEquals("resultados_rapido.csv", rapido.getFileName().toString());
        assertEquals("resultados.csv", completo.getFileName().toString());
        assertEquals("resultados_escalabilidade.csv", escalabilidade.getFileName().toString());
    }
}
