
package br.ufpb.jwpersonal.application.evento;

import br.ufpb.jwpersonal.application.service.Autorizador;
import br.ufpb.jwpersonal.application.service.AtribuicaoService;
import br.ufpb.jwpersonal.application.service.SessaoUsuario;
import br.ufpb.jwpersonal.domain.excecao.ValorInvalidoException;
import br.ufpb.jwpersonal.domain.model.*;
import br.ufpb.jwpersonal.infra.memoria.*;
import br.ufpb.jwpersonal.repository.UnidadeDeTrabalho;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes do padrão Observer e da integração com AtribuicaoService.
 */
class ObserverTest {

    private MemoriaUsuarioRepository usuarios;
    private MemoriaPlanoRepository planos;
    private MemoriaAtribuicaoRepository atribuicoes;

    private MemoriaUnidadeDeTrabalho unidadeMemoria;

    private PublicadorDeEventos publicador;
    private NotificadorAluno notificador;

    private AtribuicaoService atribuicaoService;

    private Usuario personal;
    private Usuario aluno;

    private SessaoUsuario sessaoPersonal;

    private PlanoTreino planoPublicado;

    private AtomicBoolean transacaoConfirmada;

    @BeforeEach
    void preparar() {

        usuarios = new MemoriaUsuarioRepository();
        planos = new MemoriaPlanoRepository();
        atribuicoes = new MemoriaAtribuicaoRepository();

        unidadeMemoria =
                new MemoriaUnidadeDeTrabalho(atribuicoes);

        publicador = new PublicadorDeEventos();
        notificador = new NotificadorAluno();

        publicador.inscrever(notificador);

        transacaoConfirmada = new AtomicBoolean(false);

        personal = new Usuario(
                "Personal Teste",
                "personal.observer@teste.com",
                "hash",
                Perfil.PERSONAL
        );

        aluno = new Usuario(
                "Aluno Teste",
                "aluno.observer@teste.com",
                "hash",
                Perfil.ALUNO
        );

        usuarios.salvar(personal);
        usuarios.salvar(aluno);

        sessaoPersonal = SessaoUsuario.de(personal);

        // Cria um plano com uma versão publicada.
        Exercicio exercicio = new Exercicio(
                "Agachamento",
                "Manter a postura adequada.",
                null,
                "Referência de teste"
        );

        planoPublicado = new PlanoTreino(
                "Plano de Hipertrofia",
                ObjetivoPlano.HIPERTROFIA,
                personal.getId()
        );

        Treino treino = new Treino("Treino A");

        treino.adicionarItem(new ItemTreino(
                exercicio.getId(),
                new Series(3),
                new Repeticoes(10),
                new Carga(new BigDecimal("40"))
        ));

        planoPublicado.versaoPorNumero(1)
                .adicionarTreino(treino);

        planoPublicado.publicar(1);

        planos.salvar(planoPublicado);

        /*
         * Envolve a unidade de trabalho em uma implementação
         * que registra quando ela terminou com sucesso.
         *
         * O publicador só deverá ser chamado depois desse momento.
         */
        UnidadeDeTrabalho unidadeObservavel =
                new UnidadeDeTrabalho() {

                    @Override
                    public <T> T executar(Supplier<T> operacao) {

                        T resultado = unidadeMemoria.executar(operacao);

                        // Só chega aqui se a operação terminou normalmente.
                        transacaoConfirmada.set(true);

                        return resultado;
                    }
                };

        atribuicaoService = new AtribuicaoService(
                atribuicoes,
                planos,
                usuarios,
                unidadeObservavel,
                new Autorizador(),
                publicador
        );
    }

    /**
     * Verifica se o publicador entrega o evento
     * aos observadores inscritos.
     */
    @Test
    void publicadorEntregaEventoAosObservadores() {

        List<PlanoAtribuidoEvento> recebidos =
                new ArrayList<>();

        ObservadorDeEventos observador = recebidos::add;

        publicador.inscrever(observador);

        PlanoAtribuidoEvento evento = criarEvento();

        publicador.publicar(evento);

        assertEquals(1, recebidos.size());

        assertEquals(
                evento,
                recebidos.get(0)
        );
    }

    /**
     * Verifica se o notificador armazena a notificação
     * apenas para o aluno destinatário.
     */
    @Test
    void notificadorArmazenaNotificacaoParaAlunoCorreto() {

        PlanoAtribuidoEvento evento = criarEvento();

        notificador.aoReceber(evento);

        assertEquals(
                1,
                notificador.notificacoesDoAluno(
                        aluno.getId()
                ).size()
        );

        assertTrue(
                notificador.notificacoesDoAluno(
                        UUID.randomUUID()
                ).isEmpty()
        );

        assertEquals(
                evento.mensagem(),
                notificador.notificacoesDoAluno(
                        aluno.getId()
                ).get(0).mensagem()
        );
    }

    /**
     * Verifica se a atribuição gera a notificação
     * somente depois da conclusão da transação.
     */
    @Test
    void atribuicaoNotificaDepoisDaTransacao() {

        AtomicBoolean eventoRecebido =
                new AtomicBoolean(false);

        AtomicBoolean eventoAntesDoCommit =
                new AtomicBoolean(false);

        publicador.inscrever(evento -> {

            eventoRecebido.set(true);

            if (!transacaoConfirmada.get()) {
                eventoAntesDoCommit.set(true);
            }
        });

        Atribuicao atribuicao = atribuicaoService.atribuir(
                sessaoPersonal,
                aluno.getId(),
                planoPublicado.getId(),
                1
        );

        assertTrue(atribuicao.estaAtiva());

        assertTrue(transacaoConfirmada.get());

        assertTrue(eventoRecebido.get());

        assertFalse(eventoAntesDoCommit.get());

        assertEquals(
                1,
                notificador.notificacoesDoAluno(
                        aluno.getId()
                ).size()
        );

        PlanoAtribuidoEvento evento =
                notificador.notificacoesDoAluno(
                        aluno.getId()
                ).get(0);

        assertEquals(
                planoPublicado.getId(),
                evento.planoId()
        );

        assertEquals(
                planoPublicado.getTitulo(),
                evento.tituloPlano()
        );
    }

    /**
     * Verifica se repetir a mesma atribuição
     * não produz uma segunda notificação.
     */
    @Test
    void atribuicaoRepetidaNaoGeraNovaNotificacao() {

        atribuicaoService.atribuir(
                sessaoPersonal,
                aluno.getId(),
                planoPublicado.getId(),
                1
        );

        atribuicaoService.atribuir(
                sessaoPersonal,
                aluno.getId(),
                planoPublicado.getId(),
                1
        );

        assertEquals(
                1,
                notificador.notificacoesDoAluno(
                        aluno.getId()
                ).size()
        );

        assertEquals(
                1,
                atribuicoes.historicoDoAluno(
                        aluno.getId()
                ).size()
        );
    }

    /**
     * Verifica que uma atribuição inválida
     * não produz uma notificação.
     */
    @Test
    void atribuicaoFalhaNaoGeraNotificacao() {

        // Plano novo, ainda em RASCUNHO.
        PlanoTreino rascunho = new PlanoTreino(
                "Plano em rascunho",
                ObjetivoPlano.HIPERTROFIA,
                personal.getId()
        );

        planos.salvar(rascunho);

        assertThrows(
                ValorInvalidoException.class,
                () -> atribuicaoService.atribuir(
                        sessaoPersonal,
                        aluno.getId(),
                        rascunho.getId(),
                        1
                )
        );

        assertTrue(
                notificador.notificacoesDoAluno(
                        aluno.getId()
                ).isEmpty()
        );

        assertTrue(
                atribuicoes.buscarAtivaDoAluno(
                        aluno.getId()
                ).isEmpty()
        );
    }

    /**
     * Verifica que a falha de um observador
     * não impede que os demais recebam o evento.
     */
    @Test
    void falhaDeUmObservadorNaoImpedeOsDemais() {

        PublicadorDeEventos outroPublicador =
                new PublicadorDeEventos();

        List<PlanoAtribuidoEvento> recebidos =
                new ArrayList<>();

        // Primeiro observador apresenta uma falha.
        outroPublicador.inscrever(evento -> {
            throw new IllegalStateException(
                    "Falha simulada no observador."
            );
        });

        // Segundo observador deve receber normalmente.
        outroPublicador.inscrever(recebidos::add);

        PlanoAtribuidoEvento evento = criarEvento();

        assertDoesNotThrow(
                () -> outroPublicador.publicar(evento)
        );

        assertEquals(1, recebidos.size());

        assertEquals(evento, recebidos.get(0));
    }

    /**
     * Cria um evento para os testes do Observer.
     */
    private PlanoAtribuidoEvento criarEvento() {

        return new PlanoAtribuidoEvento(
                aluno.getId(),
                planoPublicado.getId(),
                1,
                planoPublicado.getTitulo(),
                LocalDate.now()
        );
    }
}
