package br.ufpb.jwpersonal.domain.fabrica;

import br.ufpb.jwpersonal.domain.model.ObjetivoPlano;

import java.util.List;

/**
 * Gerador concreto de modelos iniciais para planos de hipertrofia.
 */
public final class GeradorHipertrofia extends GeradorDePlano {

    /**
     * Cria um modelo inicial contendo três sessões de treino.
     */
    @Override
    protected ModeloInicialPlano criarModeloInicial() {

        return new ModeloInicialPlano(
                ObjetivoPlano.HIPERTROFIA,
                List.of(
                        "Treino A",
                        "Treino B",
                        "Treino C"
                )
        );
    }
}