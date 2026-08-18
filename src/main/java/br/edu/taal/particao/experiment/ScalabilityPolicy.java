package br.edu.taal.particao.experiment;

import br.edu.taal.particao.algorithms.PartitionAlgorithm;

import java.util.HashMap;
import java.util.Map;

/**
 * Interrompe de forma adaptativa combinacoes algoritmo/perfil que ja
 * demonstraram inviabilidade consecutiva no experimento de escalabilidade.
 * Isso preserva o ponto em que o limite foi observado sem desperdiçar tempo
 * repetindo casos ainda maiores que previsivelmente terao o mesmo resultado.
 */
public class ScalabilityPolicy {

    private final int limiteFalhasConsecutivas;
    private final Map<String, Integer> falhasConsecutivas = new HashMap<>();
    private final Map<String, String> bloqueios = new HashMap<>();

    public ScalabilityPolicy(int limiteFalhasConsecutivas) {
        if (limiteFalhasConsecutivas <= 0) {
            throw new IllegalArgumentException("O limite de falhas consecutivas deve ser positivo.");
        }
        this.limiteFalhasConsecutivas = limiteFalhasConsecutivas;
    }

    /** Retorna o motivo do bloqueio ou {@code null} quando a combinacao ainda deve ser executada. */
    public String getMotivoBloqueio(String perfil, PartitionAlgorithm algoritmo) {
        return bloqueios.get(chave(perfil, algoritmo.getNome()));
    }

    /** Atualiza a politica a partir do resultado mais recente. */
    public void registrar(ExecutionRecord registro) {
        String chave = chave(registro.getPerfil(), registro.getNomeAlgoritmo());

        if (registro.isSucesso()) {
            falhasConsecutivas.remove(chave);
            return;
        }

        boolean inviavel = registro.getStatus() == ExecutionRecord.Status.TEMPO_LIMITE
                || registro.getStatus() == ExecutionRecord.Status.MEMORIA_INVIAVEL;
        if (!inviavel) {
            falhasConsecutivas.remove(chave);
            return;
        }

        int quantidade = falhasConsecutivas.merge(chave, 1, Integer::sum);
        if (quantidade >= limiteFalhasConsecutivas) {
            bloqueios.put(chave, String.format(
                    "Nao executado apos %d inviabilidades consecutivas; ultima ocorrencia: %s em %s.",
                    limiteFalhasConsecutivas, registro.getStatus(), registro.getNomeInstancia()));
        }
    }

    private String chave(String perfil, String algoritmo) {
        return perfil + '\u0000' + algoritmo;
    }
}
