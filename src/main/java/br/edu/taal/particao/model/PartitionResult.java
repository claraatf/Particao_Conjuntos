package br.edu.taal.particao.model;

/**
 * Resultado produzido por um algoritmo de particao: a atribuicao de cada
 * elemento a um dos dois subconjuntos (grupo[i] == true significa que o
 * elemento de indice i pertence ao subconjunto A), as somas resultantes,
 * a diferenca entre elas e as metricas coletadas durante a execucao.
 */
public class PartitionResult {

    private final String nomeAlgoritmo;
    private final boolean[] grupo;
    private final long somaA;
    private final long somaB;
    private final Metrics metricas;

    public PartitionResult(String nomeAlgoritmo, boolean[] grupo, long somaA, long somaB, Metrics metricas) {
        this.nomeAlgoritmo = nomeAlgoritmo;
        this.grupo = grupo;
        this.somaA = somaA;
        this.somaB = somaB;
        this.metricas = metricas;
    }

    public String getNomeAlgoritmo() {
        return nomeAlgoritmo;
    }

    public boolean[] getGrupo() {
        return grupo;
    }

    public long getSomaA() {
        return somaA;
    }

    public long getSomaB() {
        return somaB;
    }

    public long getDiferenca() {
        return Math.abs(somaA - somaB);
    }

    public Metrics getMetricas() {
        return metricas;
    }

    /** GAP percentual em relacao a uma solucao considerada otima (ou de referencia). */
    public double calcularGapPercentual(long diferencaOtima) {
        if (diferencaOtima == 0) {
            return getDiferenca() == 0 ? 0.0 : 100.0;
        }
        return 100.0 * (getDiferenca() - diferencaOtima) / diferencaOtima;
    }

    @Override
    public String toString() {
        return String.format("[%s] somaA=%d, somaB=%d, diferenca=%d | %s",
                nomeAlgoritmo, somaA, somaB, getDiferenca(), metricas);
    }
}
