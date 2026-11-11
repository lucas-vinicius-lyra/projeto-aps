package br.ufpb.jwpersonal.domain.fabrica;

import java.util.Objects;

/**
 * Criador abstrato do padrão Factory Method.
 *
 * Define o fluxo de criação de um modelo inicial de plano,
 * delegando a construção às subclasses concretas.
 */
public abstract class GeradorDePlano {

    /**
     * Operação pública que solicita a criação do modelo inicial.
     *
     * A criação efetiva é delegada ao método sobrescrevível
     * criarModeloInicial(), implementado pelas subclasses.
     */
    public final ModeloInicialPlano gerarModeloInicial() {

        return Objects.requireNonNull(
                criarModeloInicial(),
                "O gerador deve produzir um modelo inicial válido."
        );
    }

    /**
     * Factory Method.
     *
     * Cada gerador concreto implementa sua própria forma
     * de construir um modelo inicial de plano.
     */
    protected abstract ModeloInicialPlano criarModeloInicial();
}