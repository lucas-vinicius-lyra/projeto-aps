package br.ufpb.jwpersonal.domain.fabrica;

import br.ufpb.jwpersonal.domain.model.ObjetivoPlano;

import java.util.List;

/**
 * Gerador concreto de modelos iniciais para planos de emagrecimento.
 */
public final class GeradorEmagrecimento extends GeradorDePlano {

    /**
     * Cria um modelo inicial contendo duas sessões de treino.
     */
    @Override
    protected ModeloInicialPlano criarModeloInicial() {

        return new ModeloInicialPlano(
                ObjetivoPlano.EMAGRECIMENTO,
                List.of(
                        "Treino A",
                        "Treino B"
                )
        );
    }
}