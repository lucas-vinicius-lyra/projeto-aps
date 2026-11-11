package br.ufpb.jwpersonal.domain.politica;

import br.ufpb.jwpersonal.domain.excecao.ValorInvalidoException;
import br.ufpb.jwpersonal.domain.model.Carga;
import br.ufpb.jwpersonal.domain.model.ItemTreino;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Estratégia que aumenta a carga prescrita em um percentual configurável.
 * O percentual padrão é de 5%.
 */
public final class ProgressaoCarga implements PoliticaProgressao {

    private static final BigDecimal PERCENTUAL_PADRAO =
            new BigDecimal("5");

    private final BigDecimal percentual;

    /**
     * Cria a estratégia utilizando o percentual padrão de 5%.
     */
    public ProgressaoCarga() {
        this(PERCENTUAL_PADRAO);
    }

    /**
     * Cria a estratégia utilizando um percentual personalizado.
     *
     * Exemplo: 7.5 representa um aumento de 7,5%.
     */
    public ProgressaoCarga(BigDecimal percentual) {

        Objects.requireNonNull(
                percentual,
                "O percentual é obrigatório."
        );

        if (percentual.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValorInvalidoException(
                    "O percentual de progressão não pode ser negativo."
            );
        }

        this.percentual = percentual.stripTrailingZeros();
    }

    /**
     * Aplica a progressão de carga ao exercício.
     */
    @Override
    public ItemTreino progredir(ItemTreino item) {

        Objects.requireNonNull(
                item,
                "O item de treino é obrigatório."
        );

        Carga novaCarga = item.carga()
                .aumentarPercentual(percentual);

        return item.comCarga(novaCarga);
    }

    public BigDecimal getPercentual() {
        return percentual;
    }
}