
package br.ufpb.jwpersonal.application.evento;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Evento publicado quando um plano é atribuído a um aluno.
 */
public record PlanoAtribuidoEvento(
        UUID alunoId,
        UUID planoId,
        int numeroVersao,
        String tituloPlano,
        LocalDate dataAtribuicao
) {

    public PlanoAtribuidoEvento {

        Objects.requireNonNull(alunoId);
        Objects.requireNonNull(planoId);
        Objects.requireNonNull(dataAtribuicao);

        if (numeroVersao < 1) {
            throw new IllegalArgumentException(
                    "O número da versão deve ser positivo."
            );
        }

        if (tituloPlano == null || tituloPlano.isBlank()) {
            throw new IllegalArgumentException(
                    "O título do plano é obrigatório."
            );
        }

        tituloPlano = tituloPlano.trim();
    }

    /**
     * Monta a mensagem que será apresentada ao aluno.
     */
    public String mensagem() {
        return "Um novo plano de treino foi atribuído: "
                + tituloPlano
                + " (versão "
                + numeroVersao
                + ").";
    }
}
