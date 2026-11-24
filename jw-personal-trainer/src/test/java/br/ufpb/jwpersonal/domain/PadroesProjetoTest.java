
package br.ufpb.jwpersonal.domain;

import br.ufpb.jwpersonal.domain.model.*;
import br.ufpb.jwpersonal.domain.politica.*;
import br.ufpb.jwpersonal.domain.fabrica.*;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PadroesProjetoTest {

    private ItemTreino criarItem(int series, String carga) {
        return new ItemTreino(
                UUID.randomUUID(),
                new Series(series),
                new Repeticoes(12),
                new Carga(new BigDecimal(carga))
        );
    }

    @Test
    void progressaoCargaAumentaCincoPorCento() {
        ItemTreino original = criarItem(3, "20");

        ItemTreino resultado =
                new ProgressaoCarga().progredir(original);

        assertEquals(0, resultado.carga().valor()
                .compareTo(new BigDecimal("21")));

        assertEquals(3, resultado.series().valor());
        assertEquals(0, original.carga().valor()
                .compareTo(new BigDecimal("20")));
    }

    @Test
    void progressaoVolumeAdicionaUmaSerie() {
        ItemTreino original = criarItem(3, "20");

        ItemTreino resultado =
                new ProgressaoVolume().progredir(original);

        assertEquals(4, resultado.series().valor());
        assertEquals(3, original.series().valor());
    }

    @Test
    void progressaoVolumeRespeitaLimite() {
        ItemTreino original = criarItem(10, "20");

        ItemTreino resultado =
                new ProgressaoVolume().progredir(original);

        assertEquals(10, resultado.series().valor());
    }

    @Test
    void factoryHipertrofiaCriaTresTreinos() {
        GeradorDePlano gerador = new GeradorHipertrofia();

        ModeloInicialPlano modelo =
                gerador.gerarModeloInicial();

        assertEquals(ObjetivoPlano.HIPERTROFIA,
                modelo.objetivo());

        assertEquals(3, modelo.criarTreinos().size());
    }

    @Test
    void factoryEmagrecimentoCriaDoisTreinos() {
        GeradorDePlano gerador = new GeradorEmagrecimento();

        ModeloInicialPlano modelo =
                gerador.gerarModeloInicial();

        assertEquals(ObjetivoPlano.EMAGRECIMENTO,
                modelo.objetivo());

        assertEquals(2, modelo.criarTreinos().size());
    }
}
