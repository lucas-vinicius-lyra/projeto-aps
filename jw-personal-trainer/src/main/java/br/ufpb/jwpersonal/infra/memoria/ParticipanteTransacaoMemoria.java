
package br.ufpb.jwpersonal.infra.memoria;

/**
 * Contrato para repositórios que participam de transações
 * na implementação em memória.
 *
 * Permite criar um snapshot do estado atual e restaurá-lo
 * caso ocorra uma falha durante uma operação.
 */
public interface ParticipanteTransacaoMemoria {

    /**
     * Cria uma cópia do estado atual do repositório.
     */
    Object criarSnapshot();

    /**
     * Restaura o estado anterior do repositório.
     */
    void restaurarSnapshot(Object snapshot);
}
