package br.edu.taal.particao.algorithms;

/**
 * Lancada quando a tabela de Programacao Dinamica exigida para resolver
 * uma instancia seria grande demais para alocar com seguranca (o classico
 * problema de "pseudo-polinomialidade": a tabela depende do VALOR da soma
 * dos elementos, nao apenas da quantidade deles). Isso permite que o
 * executor de experimentos registre a inviabilidade em vez de derrubar a
 * JVM com OutOfMemoryError.
 */
public class TabelaInviavelException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TabelaInviavelException(String mensagem) {
        super(mensagem);
    }
}
