package br.ufpb.jwpersonal.domain.politica;

import br.ufpb.jwpersonal.domain.excecao.ValorInvalidoException;
import br.ufpb.jwpersonal.domain.model.ItemTreino;
import br.ufpb.jwpersonal.domain.model.Series;

import java.util.Objects;

/**
 * Estratégia que aumenta em uma unidade a quantidade de séries
 * de cada exercício, respeitando o limite máximo configurado.
 */
public final class ProgressaoVolume implements PoliticaProgressao {

    private static final int TETO_PADRAO = 10;

    private final int tetoSeries;

    /**
     * Cria a estratégia com o limite padrão de 10 séries.
     */
    public ProgressaoVolume() {
        this(TETO_PADRAO);
    }

    /**
     * Cria a estratégia com um teto de séries personalizado.
     */
    public ProgressaoVolume(int tetoSeries) {

        if (tetoSeries < 1 || tetoSeries > TETO_PADRAO) {
            throw new ValorInvalidoException(
                    "O teto de séries deve estar entre 1 e 10."
            );
        }

        this.tetoSeries = tetoSeries;
    }

    /**
     * Aplica a progressão de volume ao exercício.
     */
    @Override
    public ItemTreino progredir(ItemTreino item) {

        Objects.requireNonNull(
                item,
                "O item de treino é obrigatório."
        );

        int seriesAtuais = item.series().valor();

        // Não permite ultrapassar o limite nem reduzir
        // uma prescrição que já esteja acima de um teto personalizado.
        if (seriesAtuais >= tetoSeries) {
            return item;
        }

        int novasSeries = seriesAtuais + 1;

        return item.comSeries(new Series(novasSeries));
    }

    public int getTetoSeries() {
        return tetoSeries;
    }
}