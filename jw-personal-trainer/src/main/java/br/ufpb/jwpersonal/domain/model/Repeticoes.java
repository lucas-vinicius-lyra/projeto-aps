package br.ufpb.jwpersonal.domain.model;

import br.ufpb.jwpersonal.domain.excecao.ValorInvalidoException;

/**
 * Objeto de valor que representa a quantidade de repetições de um exercício (RN07).
 */
public record Repeticoes(int valor) {

    public Repeticoes {

        if (valor < 1 || valor > 50) {
            throw new ValorInvalidoException(
                    "A quantidade de repetições deve estar entre 1 e 50.");
        }
    }
}