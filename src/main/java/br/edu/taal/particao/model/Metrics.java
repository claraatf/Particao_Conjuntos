package br.edu.taal.particao.model;

/**
 * Contadores e medidas coletados durante a execucao de um algoritmo.
 * Os campos de contagem (estados explorados, chamadas recursivas, podas,
 * profundidade maxima) sao incrementados pelos proprios algoritmos e nem
 * todos se aplicam a todas as estrategias (ex.: Programacao Dinamica nao
 * realiza "podas" no sentido de branch-and-bound).
 */
public class Metrics {

    private long tempoExecucaoNanos;
    private long memoriaUsadaBytes;

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
    }

    public void setMemoriaUsadaBytes(long memoriaUsadaBytes) {
        this.memoriaUsadaBytes = memoriaUsadaBytes;
    }

    public long getTempoExecucaoNanos() {
        return tempoExecucaoNanos;
    }

    public double getTempoExecucaoMillis() {
        return tempoExecucaoNanos / 1_000_000.0;
    }

    public long getMemoriaUsadaBytes() {
        return memoriaUsadaBytes;
    }

    public double getMemoriaUsadaMB() {
        return memoriaUsadaBytes / (1024.0 * 1024.0);
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
        return String.format(
                "tempo=%.3fms, memoria=%.3fMB, estados=%d, chamadasRecursivas=%d, podas=%d, profMax=%d",
                getTempoExecucaoMillis(), getMemoriaUsadaMB(), estadosExplorados,
                chamadasRecursivas, podasRealizadas, profundidadeMaxima);
    }
}
