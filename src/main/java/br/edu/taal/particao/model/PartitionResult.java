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
    public Double calcularGapPercentual(long diferencaOtima) {
        if (diferencaOtima == 0) {
            // GAP relativo e indefinido quando o otimo e zero (divisao por zero).
            // Retornar um valor arbitrario aqui mascararia a qualidade real da
            // solucao: usa-se o GAP absoluto e o desequilibrio relativo.
            return getDiferenca() == 0 ? Double.valueOf(0.0) : null;
        }
        return 100.0 * (getDiferenca() - diferencaOtima) / diferencaOtima;
    }

    /** Diferenca absoluta em relacao ao otimo, sempre bem definida. */
    public long calcularGapAbsoluto(long diferencaOtima) {
        return getDiferenca() - diferencaOtima;
    }

    public long getSomaTotal() {
        return somaA + somaB;
    }

    /**
     * Desequilibrio da particao como percentual da soma total. Ao contrario do
     * GAP relativo, e sempre definido e permanece comparavel entre instancias
     * de escalas diferentes, o que o torna a metrica de qualidade mais estavel
     * quando o otimo e zero ou muito proximo de zero.
     */
    public double getDesequilibrioRelativo() {
        long total = getSomaTotal();
        return total == 0 ? 0.0 : 100.0 * getDiferenca() / total;
    }

    @Override
    public String toString() {
        return String.format("[%s] somaA=%d, somaB=%d, diferenca=%d | %s",
                nomeAlgoritmo, somaA, somaB, getDiferenca(), metricas);
    }
}
