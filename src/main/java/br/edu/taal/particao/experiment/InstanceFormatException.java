package br.edu.taal.particao.experiment;

/**
 * Lancada quando um arquivo de instancias fornecido pelo usuario nao pode ser
 * interpretado. A mensagem sempre identifica o arquivo, a linha e o trecho
 * problematico, de modo que o erro possa ser corrigido sem inspecionar o
 * codigo-fonte.
 */
public class InstanceFormatException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InstanceFormatException(String mensagem) {
        super(mensagem);
    }
}
