package br.edu.taal.particao.algorithms;

/**
 * Lancada quando a execucao de um algoritmo e interrompida por ter
 * ultrapassado o tempo limite definido pelo experimento. Os algoritmos
 * exponenciais verificam periodicamente o sinal de interrupcao da thread
 * para que instancias inviaveis nao travem a bateria de testes.
 */
public class TempoLimiteExcedidoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TempoLimiteExcedidoException(String mensagem) {
        super(mensagem);
    }
}
