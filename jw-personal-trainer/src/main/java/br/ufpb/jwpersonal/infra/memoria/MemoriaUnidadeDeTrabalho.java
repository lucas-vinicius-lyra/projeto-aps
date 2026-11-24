
package br.ufpb.jwpersonal.infra.memoria;

import br.ufpb.jwpersonal.repository.UnidadeDeTrabalho;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Implementação de UnidadeDeTrabalho utilizando memória.
 *
 * Antes de executar uma operação, cria snapshots dos
 * repositórios participantes.
 *
 * Caso ocorra uma falha, restaura os estados anteriores.
 */
public class MemoriaUnidadeDeTrabalho
        implements UnidadeDeTrabalho {

    private final List<ParticipanteTransacaoMemoria> participantes;

    public MemoriaUnidadeDeTrabalho(
            ParticipanteTransacaoMemoria... participantes) {

        if (participantes == null || participantes.length == 0) {
            throw new IllegalArgumentException(
                    "Informe ao menos um participante da transação."
            );
        }

        this.participantes = List.copyOf(
                Arrays.asList(participantes)
        );
    }

    /**
     * Executa uma operação com suporte a rollback.
     */
    @Override
    public synchronized <T> T executar(
            Supplier<T> operacao) {

        Objects.requireNonNull(
                operacao,
                "A operação é obrigatória."
        );

        // 1. Salvar o estado inicial dos participantes.
        List<Object> snapshots = new ArrayList<>();

        for (ParticipanteTransacaoMemoria participante
                : participantes) {

            snapshots.add(participante.criarSnapshot());
        }

        try {

            // 2. Executar a operação.
            // Se terminar normalmente, considera-se confirmada.
            return operacao.get();

        } catch (RuntimeException | Error falha) {

            // 3. Se ocorrer falha, restaurar todos os participantes
            // em ordem inversa.
            for (int i = participantes.size() - 1; i >= 0; i--) {

                try {

                    participantes.get(i)
                            .restaurarSnapshot(snapshots.get(i));

                } catch (RuntimeException | Error erroRollback) {

                    // Preserva a falha original e registra
                    // também eventual erro durante a restauração.
                    falha.addSuppressed(erroRollback);
                }
            }

            throw falha;
        }
    }
}
