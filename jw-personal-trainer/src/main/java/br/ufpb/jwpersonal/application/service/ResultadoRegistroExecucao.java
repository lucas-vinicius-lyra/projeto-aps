
package br.ufpb.jwpersonal.application.service;

import br.ufpb.jwpersonal.domain.model.Execucao;

import java.util.Objects;

/**
 * Representa o resultado de uma tentativa de registrar
 * a conclusão de um treino.
 *
 * Informa se a execução foi criada agora ou se já existia.
 */
public record ResultadoRegistroExecucao(
        Execucao execucao,
        boolean jaExistia
) {

    public ResultadoRegistroExecucao {
        Objects.requireNonNull(
                execucao,
                "A execução é obrigatória."
        );
    }

    /**
     * Informa se a execução foi registrada nesta tentativa.
     */
    public boolean foiRegistradaAgora() {
        return !jaExistia;
    }
}
