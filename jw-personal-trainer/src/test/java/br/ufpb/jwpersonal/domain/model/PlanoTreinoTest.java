
package br.ufpb.jwpersonal.domain.model;

import br.ufpb.jwpersonal.domain.excecao.TransicaoInvalidaException;
import br.ufpb.jwpersonal.domain.excecao.ValorInvalidoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlanoTreinoTest {

    private PlanoTreino plano;

    private Treino criarTreino(String nome) {
        Treino treino = new Treino(nome);

        treino.adicionarItem(new ItemTreino(
                UUID.randomUUID(),
                new Series(3),
                new Repeticoes(12),
                new Carga(new BigDecimal("20"))
        ));

        return treino;
    }

    @BeforeEach
    void preparar() {
        plano = new PlanoTreino(
                "Hipertrofia",
                ObjetivoPlano.HIPERTROFIA,
                UUID.randomUUID()
        );
    }

    @Test
    void planoComecaComVersaoUmEmRascunho() {
        assertEquals(1, plano.historico().size());
        assertEquals(EstadoVersao.RASCUNHO,
                plano.versaoPorNumero(1).getEstado());
    }

    @Test
    void publicaPrimeiraVersao() {
        plano.versaoPorNumero(1)
                .adicionarTreino(criarTreino("Treino A"));

        plano.publicar(1);

        assertEquals(EstadoVersao.PUBLICADA,
                plano.versaoPorNumero(1).getEstado());

        assertTrue(plano.versaoPublicada().isPresent());
    }

    @Test
    void naoPublicaPlanoSemTreino() {
        assertThrows(ValorInvalidoException.class,
                () -> plano.publicar(1));

        assertEquals(EstadoVersao.RASCUNHO,
                plano.versaoPorNumero(1).getEstado());
    }

    @Test
    void revisaoCriaNovaVersaoSemAlterarAnterior() {
        plano.versaoPorNumero(1)
                .adicionarTreino(criarTreino("Treino A"));

        plano.publicar(1);

        VersaoPlano revisao = plano.iniciarRevisao();

        assertEquals(2, revisao.getNumero());
        assertEquals(EstadoVersao.RASCUNHO,
                revisao.getEstado());

        revisao.adicionarTreino(criarTreino("Treino B"));

        assertEquals(1,
                plano.versaoPorNumero(1).getTreinos().size());

        assertEquals(2, revisao.getTreinos().size());
    }

    @Test
    void naoPermiteDoisRascunhosAbertos() {
        plano.versaoPorNumero(1)
                .adicionarTreino(criarTreino("Treino A"));

        plano.publicar(1);
        plano.iniciarRevisao();

        assertThrows(TransicaoInvalidaException.class,
                () -> plano.iniciarRevisao());
    }

    @Test
    void publicarRevisaoArquivaVersaoAnterior() {
        plano.versaoPorNumero(1)
                .adicionarTreino(criarTreino("Treino A"));

        plano.publicar(1);
        plano.iniciarRevisao();
        plano.publicar(2);

        assertEquals(EstadoVersao.ARQUIVADA,
                plano.versaoPorNumero(1).getEstado());

        assertEquals(EstadoVersao.PUBLICADA,
                plano.versaoPorNumero(2).getEstado());

        assertEquals(2, plano.historico().size());
    }

    @Test
    void impedeCorrecaoSeExistirExecucao() {
        VersaoPlano versao = plano.versaoPorNumero(1);

        versao.adicionarTreino(criarTreino("Treino A"));
        plano.publicar(1);

        assertThrows(TransicaoInvalidaException.class,
                () -> versao.corrigir(
                        List.of(criarTreino("Treino B")),
                        true
                ));
    }

    @Test
    void permiteCorrecaoSemExecucao() {
        VersaoPlano versao = plano.versaoPorNumero(1);

        versao.adicionarTreino(criarTreino("Treino A"));
        plano.publicar(1);

        versao.corrigir(
                List.of(criarTreino("Treino B")),
                false
        );

        assertEquals("Treino B",
                versao.getTreinos().get(0).getNome());
    }
}
