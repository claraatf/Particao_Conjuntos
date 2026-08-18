package br.edu.taal.particao.model;

/**
 * Contadores e medidas coletados durante a execucao de um algoritmo. A
 * memoria representa os bytes alocados no heap pela thread durante a chamada,
 * e nao apenas a variacao de objetos vivos observada ao final.
 * Os campos de contagem (estados explorados, chamadas recursivas, podas,
 * profundidade maxima) sao incrementados pelos proprios algoritmos e nem
 * todos se aplicam a todas as estrategias (ex.: Programacao Dinamica nao
 * realiza "podas" no sentido de branch-and-bound).
 */
public class Metrics {

    private long tempoExecucaoNanos;
    private long tempoMinimoNanos;
    private long tempoMaximoNanos;
    private double desvioPadraoTempoNanos;
    private int repeticoesMedidas;
    /** Bytes alocados no heap pela thread durante a execucao; -1 quando indisponivel. */
    private long memoriaAlocadaBytes = -1L;

    private long estadosExplorados;
    private long chamadasRecursivas;
    private long podasRealizadas;
    private int profundidadeMaxima;

    public void incrementarEstadosExplorados() {
        estadosExplorados++;
    }

    public void incrementarChamadasRecursivas() {
        chamadasRecursivas++;
    }

    public void incrementarPodas() {
        podasRealizadas++;
    }

    public void registrarProfundidade(int profundidade) {
        if (profundidade > profundidadeMaxima) {
            profundidadeMaxima = profundidade;
        }
    }

    public void setTempoExecucaoNanos(long tempoExecucaoNanos) {
        this.tempoExecucaoNanos = tempoExecucaoNanos;
        this.tempoMinimoNanos = tempoExecucaoNanos;
        this.tempoMaximoNanos = tempoExecucaoNanos;
        this.desvioPadraoTempoNanos = 0.0;
        this.repeticoesMedidas = 1;
    }

    /** Registra o resumo estatistico das repeticoes medidas pelo executor. */
    public void setResumoTempos(long medianaNanos, long minimoNanos, long maximoNanos,
                                double desvioPadraoNanos, int repeticoesMedidas) {
        this.tempoExecucaoNanos = medianaNanos;
        this.tempoMinimoNanos = minimoNanos;
        this.tempoMaximoNanos = maximoNanos;
        this.desvioPadraoTempoNanos = desvioPadraoNanos;
        this.repeticoesMedidas = repeticoesMedidas;
    }

    public void setMemoriaAlocadaBytes(long memoriaAlocadaBytes) {
        this.memoriaAlocadaBytes = memoriaAlocadaBytes;
    }

    public long getTempoExecucaoNanos() {
        return tempoExecucaoNanos;
    }

    public double getTempoExecucaoMillis() {
        return tempoExecucaoNanos / 1_000_000.0;
    }

    public double getTempoMinimoMillis() {
        return tempoMinimoNanos / 1_000_000.0;
    }

    public double getTempoMaximoMillis() {
        return tempoMaximoNanos / 1_000_000.0;
    }

    public double getDesvioPadraoTempoMillis() {
        return desvioPadraoTempoNanos / 1_000_000.0;
    }

    public int getRepeticoesMedidas() {
        return repeticoesMedidas;
    }

    public long getMemoriaAlocadaBytes() {
        return memoriaAlocadaBytes;
    }

    public double getMemoriaAlocadaMB() {
        return memoriaAlocadaBytes < 0 ? Double.NaN : memoriaAlocadaBytes / (1024.0 * 1024.0);
    }

    public boolean isMemoriaAlocadaDisponivel() {
        return memoriaAlocadaBytes >= 0;
    }

    /**
     * Alias mantido para compatibilidade com codigo cliente anterior. A
     * metrica passou a representar bytes alocados, e nao variacao liquida do heap.
     */
    @Deprecated
    public void setMemoriaUsadaBytes(long memoriaUsadaBytes) {
        setMemoriaAlocadaBytes(memoriaUsadaBytes);
    }

    /** @see #getMemoriaAlocadaBytes() */
    @Deprecated
    public long getMemoriaUsadaBytes() {
        return getMemoriaAlocadaBytes();
    }

    /** @see #getMemoriaAlocadaMB() */
    @Deprecated
    public double getMemoriaUsadaMB() {
        return getMemoriaAlocadaMB();
    }

    public long getEstadosExplorados() {
        return estadosExplorados;
    }

    public long getChamadasRecursivas() {
        return chamadasRecursivas;
    }

    public long getPodasRealizadas() {
        return podasRealizadas;
    }

    public int getProfundidadeMaxima() {
        return profundidadeMaxima;
    }

    @Override
    public String toString() {
        String memoria = isMemoriaAlocadaDisponivel()
                ? String.format("%.3fMB", getMemoriaAlocadaMB())
                : "indisponivel";
        return String.format(
                "tempo=%.3fms, memoriaAlocada=%s, estados=%d, chamadasRecursivas=%d, podas=%d, profMax=%d",
                getTempoExecucaoMillis(), memoria, estadosExplorados,
                chamadasRecursivas, podasRealizadas, profundidadeMaxima);
    }
}
