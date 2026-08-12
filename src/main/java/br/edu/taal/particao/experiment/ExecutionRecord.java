package br.edu.taal.particao.experiment;

import br.edu.taal.particao.model.Instance;
import br.edu.taal.particao.model.PartitionResult;

/**
 * Uma linha da tabela de resultados: o que aconteceu ao executar um
 * algoritmo especifico sobre uma instancia especifica. Alem do resultado em
 * si, registra falhas (tempo limite ou memoria inviavel), pois saber
 * <em>onde</em> um algoritmo deixa de ser aplicavel e parte central da
 * analise experimental.
 */
public class ExecutionRecord {

    public enum Status {
        SUCESSO,
        TEMPO_LIMITE,
        MEMORIA_INVIAVEL,
        ERRO
    }

    private final String perfil;
    private final int tamanho;
    private final String nomeInstancia;
    private final String nomeAlgoritmo;
    private final boolean exato;
    private final Status status;
    private final PartitionResult resultado;
    private final String observacao;

    private Long diferencaOtimaReferencia;

    public ExecutionRecord(String perfil, int tamanho, String nomeInstancia, String nomeAlgoritmo,
                           boolean exato, Status status, PartitionResult resultado, String observacao) {
        this.perfil = perfil;
        this.tamanho = tamanho;
        this.nomeInstancia = nomeInstancia;
        this.nomeAlgoritmo = nomeAlgoritmo;
        this.exato = exato;
        this.status = status;
        this.resultado = resultado;
        this.observacao = observacao;
    }

    public static ExecutionRecord sucesso(Instance instancia, String perfil,
                                          PartitionResult resultado, boolean exato) {
        return new ExecutionRecord(perfil, instancia.getTamanho(), instancia.getNome(),
                resultado.getNomeAlgoritmo(), exato, Status.SUCESSO, resultado, "");
    }

    public static ExecutionRecord falha(Instance instancia, String perfil, String nomeAlgoritmo,
                                        boolean exato, Status status, String observacao) {
        return new ExecutionRecord(perfil, instancia.getTamanho(), instancia.getNome(),
                nomeAlgoritmo, exato, status, null, observacao);
    }

    public String getPerfil() {
        return perfil;
    }

    public int getTamanho() {
        return tamanho;
    }

    public String getNomeInstancia() {
        return nomeInstancia;
    }

    public String getNomeAlgoritmo() {
        return nomeAlgoritmo;
    }

    public boolean isExato() {
        return exato;
    }

    public Status getStatus() {
        return status;
    }

    public PartitionResult getResultado() {
        return resultado;
    }

    public String getObservacao() {
        return observacao;
    }

    public boolean isSucesso() {
        return status == Status.SUCESSO && resultado != null;
    }

    public void setDiferencaOtimaReferencia(Long diferencaOtimaReferencia) {
        this.diferencaOtimaReferencia = diferencaOtimaReferencia;
    }

    public Long getDiferencaOtimaReferencia() {
        return diferencaOtimaReferencia;
    }

    /** GAP percentual em relacao a referencia otima, ou null se indisponivel. */
    public Double getGapPercentual() {
        if (!isSucesso() || diferencaOtimaReferencia == null) {
            return null;
        }
        return resultado.calcularGapPercentual(diferencaOtimaReferencia);
    }

    /** Indica se o algoritmo atingiu exatamente a diferenca otima de referencia. */
    public Boolean atingiuOtimo() {
        if (!isSucesso() || diferencaOtimaReferencia == null) {
            return null;
        }
        return resultado.getDiferenca() == diferencaOtimaReferencia;
    }
}
