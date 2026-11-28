package br.ufpb.jwpersonal.domain.politica;

import br.ufpb.jwpersonal.domain.excecao.ValorInvalidoException;
import br.ufpb.jwpersonal.domain.model.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProgressaoDuplaTest {

    private final PoliticaProgressao politica = new ProgressaoDupla();

    private static ItemTreino item(int repeticoes, String carga) {
        return new ItemTreino(UUID.randomUUID(), new Series(3),
                new Repeticoes(repeticoes), new Carga(new BigDecimal(carga)));
    }

    private static void assertCarga(String esperada, ItemTreino item) {
        assertEquals(0, new BigDecimal(esperada).compareTo(item.carga().valor()),
                "carga obtida: " + item.carga().valor());
    }

    @Test
    void aumentaRepeticoesAntesDoLimiteSemMudarCargaNemOriginal() {
        ItemTreino original = item(10, "40");

        ItemTreino novo = politica.progredir(original);

        assertEquals(11, novo.repeticoes().valor());
        assertCarga("40", novo);
        assertEquals(3, novo.series().valor());
        assertEquals(10, original.repeticoes().valor());
    }

    @Test
    void noLimiteSuperiorAumentaCargaEVoltaAoLimiteInferior() {
        ItemTreino novo = politica.progredir(item(12, "40"));

        assertEquals(8, novo.repeticoes().valor());
        assertCarga("42", novo);
    }

    @Test
    void acimaDaFaixaTambemSobeCarga() {
        ItemTreino novo = politica.progredir(item(15, "40"));

        assertEquals(8, novo.repeticoes().valor());
        assertCarga("42", novo);
    }

    @Test
    void arredondaParaMultiploDeMeioQuilo() {
        // 33 * 1,05 = 34,65 -> 34,5
        assertCarga("34.5", politica.progredir(item(12, "33")));
        // 2 * 1,05 = 2,1 -> arredondaria para 2; garante o passo mínimo.
        assertCarga("2.5", politica.progredir(item(12, "2")));
    }

    @Test
    void respeitaCargaMaximaDeQuinhentosQuilos() {
        assertCarga("500", politica.progredir(item(12, "490")));

        ItemTreino noTeto = item(12, "500");
        assertSame(noTeto, politica.progredir(noTeto));
    }

    @Test
    void pesoCorporalNaoPerdeRepeticoes() {
        ItemTreino semCarga = item(12, "0");

        assertSame(semCarga, politica.progredir(semCarga));
    }

    @Test
    void incrementoNaoUltrapassaOLimiteSuperior() {
        PoliticaProgressao deCincoEmCinco =
                new ProgressaoDupla(8, 12, 5, new BigDecimal("10"));

        assertEquals(12, deCincoEmCinco.progredir(item(10, "20")).repeticoes().valor());
        assertCarga("22", deCincoEmCinco.progredir(item(12, "20")));
    }

    @Test
    void rejeitaParametrosInvalidos() {
        BigDecimal cinco = new BigDecimal("5");

        assertThrows(ValorInvalidoException.class, () -> new ProgressaoDupla(0, 12, 1, cinco));
        assertThrows(ValorInvalidoException.class, () -> new ProgressaoDupla(8, 51, 1, cinco));
        assertThrows(ValorInvalidoException.class, () -> new ProgressaoDupla(12, 12, 1, cinco));
        assertThrows(ValorInvalidoException.class, () -> new ProgressaoDupla(8, 12, 0, cinco));
        assertThrows(ValorInvalidoException.class,
                () -> new ProgressaoDupla(8, 12, 1, BigDecimal.ZERO));
        assertThrows(NullPointerException.class, () -> politica.progredir(null));
    }
}
