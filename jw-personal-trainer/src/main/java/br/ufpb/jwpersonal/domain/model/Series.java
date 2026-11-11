package br.ufpb.jwpersonal.domain.model;

import br.ufpb.jwpersonal.domain.excecao.ValorInvalidoException;

/**
 * Objeto de valor que representa a quantidade de séries de um exercício (RN07).
 */
public record Series(int valor) {

    public Series {

        if (valor < 1 || valor > 10) {
            throw new ValorInvalidoException(
                    "A quantidade de séries deve estar entre 1 e 10.");
        }
    }
}