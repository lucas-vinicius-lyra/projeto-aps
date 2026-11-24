
package br.ufpb.jwpersonal.domain.model;

import br.ufpb.jwpersonal.domain.excecao.ValorInvalidoException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ValoresTreinoTest {

    @Test
    void aceitaLimitesValidosDeSeries() {
        assertEquals(1, new Series(1).valor());
        assertEquals(10, new Series(10).valor());
    }

    @Test
    void rejeitaSeriesForaDosLimites() {
        assertThrows(ValorInvalidoException.class,
                () -> new Series(0));

        assertThrows(ValorInvalidoException.class,
                () -> new Series(11));
    }

    @Test
    void aceitaLimitesValidosDeRepeticoes() {
        assertEquals(1, new Repeticoes(1).valor());
        assertEquals(50, new Repeticoes(50).valor());
    }

    @Test
    void rejeitaRepeticoesInvalidas() {
        assertThrows(ValorInvalidoException.class,
                () -> new Repeticoes(0));

        assertThrows(ValorInvalidoException.class,
                () -> new Repeticoes(51));
    }

    @Test
    void aceitaCargaEntreZeroECincoCentos() {
        assertEquals(0,
                new Carga(BigDecimal.ZERO)
                        .valor().compareTo(BigDecimal.ZERO));

        assertEquals(0,
                new Carga(new BigDecimal("500"))
                        .valor().compareTo(new BigDecimal("500")));
    }

    @Test
    void rejeitaCargaForaDoIntervalo() {
        assertThrows(ValorInvalidoException.class,
                () -> new Carga(new BigDecimal("-1")));

        assertThrows(ValorInvalidoException.class,
                () -> new Carga(new BigDecimal("501")));
    }

    @Test
    void arredondaProgressaoParaMeioQuilo() {
        Carga carga = new Carga(new BigDecimal("23"));

        Carga resultado =
                carga.aumentarPercentual(new BigDecimal("5"));

        assertEquals(0, resultado.valor()
                .compareTo(new BigDecimal("24")));
    }

    @Test
    void rejeitaPercentualNegativo() {
        Carga carga = new Carga(new BigDecimal("20"));

        assertThrows(ValorInvalidoException.class,
                () -> carga.aumentarPercentual(
                        new BigDecimal("-5")));
    }
}
