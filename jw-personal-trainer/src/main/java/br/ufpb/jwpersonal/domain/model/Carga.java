package br.ufpb.jwpersonal.domain.model;

import br.ufpb.jwpersonal.domain.excecao.ValorInvalidoException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Objeto de valor para a carga prescrita, em quilogramas (RN07).
 */
public record Carga(BigDecimal valor) {

    private static final BigDecimal MINIMA = BigDecimal.ZERO;
    private static final BigDecimal MAXIMA = new BigDecimal("500");
    private static final BigDecimal MEIO_QUILO = new BigDecimal("0.5");
    private static final BigDecimal CEM = new BigDecimal("100");

    public Carga {

        Objects.requireNonNull(valor, "A carga é obrigatória.");

        if (valor.compareTo(MINIMA) < 0 || valor.compareTo(MAXIMA) > 0) {
            throw new ValorInvalidoException(
                    "A carga deve estar entre 0 e 500 kg.");
        }

        valor = valor.stripTrailingZeros();
    }

    /**
     * Aumenta a carga pelo percentual informado e arredonda para o múltiplo
     * de 0,5 kg mais próximo. O percentual 5 representa um aumento de 5%.
     */
    public Carga aumentarPercentual(BigDecimal percentual) {

        Objects.requireNonNull(percentual, "O percentual é obrigatório.");

        if (percentual.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValorInvalidoException(
                    "O percentual de progressão não pode ser negativo.");
        }

        BigDecimal fator = BigDecimal.ONE.add(
                percentual.divide(CEM, 8, RoundingMode.HALF_UP));

        BigDecimal novaCarga = valor.multiply(fator);

        BigDecimal unidadesDeMeioQuilo = novaCarga.divide(
                MEIO_QUILO, 0, RoundingMode.HALF_UP);

        BigDecimal cargaArredondada = unidadesDeMeioQuilo
                .multiply(MEIO_QUILO)
                .setScale(1, RoundingMode.HALF_UP);

        return new Carga(cargaArredondada);
    }
}