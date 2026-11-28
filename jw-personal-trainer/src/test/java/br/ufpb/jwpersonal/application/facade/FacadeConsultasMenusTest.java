package br.ufpb.jwpersonal.application.facade;

import br.ufpb.jwpersonal.application.evento.NotificadorAluno;
import br.ufpb.jwpersonal.application.evento.PublicadorDeEventos;
import br.ufpb.jwpersonal.application.service.*;
import br.ufpb.jwpersonal.domain.excecao.AutorizacaoNegadaException;
import br.ufpb.jwpersonal.domain.excecao.RecursoNaoEncontradoException;
import br.ufpb.jwpersonal.domain.excecao.TransicaoInvalidaException;
import br.ufpb.jwpersonal.domain.excecao.ValorInvalidoException;
import br.ufpb.jwpersonal.domain.fabrica.GeradorEmagrecimento;
import br.ufpb.jwpersonal.domain.fabrica.GeradorHipertrofia;
import br.ufpb.jwpersonal.domain.model.*;
import br.ufpb.jwpersonal.infra.seguranca.Pbkdf2SenhaHasher;
import br.ufpb.jwpersonal.infra.sqlite.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static br.ufpb.jwpersonal.infra.sqlite.DadosDemonstracaoSQLite.*;
import static org.junit.jupiter.api.Assertions.*;

class FacadeConsultasMenusTest {

    @TempDir
    Path pasta;

    private JWPersonalTrainerFacade facade;
    private SessaoUsuario personal;
    private SessaoUsuario outroPersonal;
    private SessaoUsuario aluno;

    @BeforeEach
    void preparar() {
        var conexao = new ConexaoSQLite(pasta.resolve("facade.db"));
        SqliteSchema.criarTabelas(conexao);

        var usuarios = new SqliteUsuarioRepository(conexao);
        var exercicios = new SqliteExercicioRepository(conexao);
        var planos = new SqlitePlanoRepository(conexao);
        var atribuicoes = new SqliteAtribuicaoRepository(conexao);
        var execucoes = new SqliteExecucaoRepository(conexao);
        var unidade = new SqliteUnidadeDeTrabalho(conexao);
        var hasher = new Pbkdf2SenhaHasher();
        var autorizador = new Autorizador();
        var publicador = new PublicadorDeEventos();
        var notificador = new NotificadorAluno();
        publicador.inscrever(notificador);

        facade = new JWPersonalTrainerFacade(
                new AutenticacaoService(usuarios, hasher),
                new CatalogoExerciciosService(exercicios, autorizador),
                new PrescricaoService(planos, exercicios, execucoes, unidade, autorizador,
                        Map.of(ObjetivoPlano.HIPERTROFIA, new GeradorHipertrofia(),
                                ObjetivoPlano.EMAGRECIMENTO, new GeradorEmagrecimento())),
                new AtribuicaoService(atribuicoes, planos, usuarios, unidade,
                        autorizador, publicador),
                new ExecucaoService(execucoes, atribuicoes, planos, unidade, autorizador),
                new ConsultaService(planos, atribuicoes, execucoes, autorizador, notificador));

        new DadosDemonstracaoSQLite(usuarios, exercicios, planos, atribuicoes,
                unidade, hasher).carregarSeNecessario();

        usuarios.salvar(new Usuario("Outro", "outro@teste.com",
                hasher.gerarHash("senha123"), Perfil.PERSONAL));

        personal = facade.autenticar(EMAIL_PERSONAL, SENHA_PERSONAL);
        outroPersonal = facade.autenticar("outro@teste.com", "senha123");
        aluno = facade.autenticar(EMAIL_ALUNO, SENHA_ALUNO);
    }

    private UUID planoDemo() {
        return facade.listarMeusPlanos(personal).get(0).getId();
    }

    @Test
    void cadaPersonalVeSomenteOsPropriosPlanosEAtribuicoes() {
        assertEquals(1, facade.listarMeusPlanos(personal).size());
        assertTrue(facade.listarMeusPlanos(outroPersonal).isEmpty());
        assertEquals(1, facade.consultarAtribuicoesDoAluno(personal, aluno.usuarioId()).size());
        assertTrue(facade.consultarAtribuicoesDoAluno(outroPersonal, aluno.usuarioId()).isEmpty());

        UUID plano = planoDemo();
        assertThrows(AutorizacaoNegadaException.class,
                () -> facade.consultarTituloDoPlano(outroPersonal, plano));
        assertThrows(AutorizacaoNegadaException.class,
                () -> facade.podeCorrigirDiretamente(outroPersonal, plano, 1));
    }

    @Test
    void alunoNaoUsaOperacoesDoPersonalEViceVersa() {
        assertThrows(AutorizacaoNegadaException.class, () -> facade.listarMeusPlanos(aluno));
        assertThrows(AutorizacaoNegadaException.class,
                () -> facade.localizarAlunoPorEmail(aluno, EMAIL_ALUNO));
        assertThrows(AutorizacaoNegadaException.class,
                () -> facade.consultarAtribuicoesDoAluno(aluno, aluno.usuarioId()));
        assertThrows(AutorizacaoNegadaException.class,
                () -> facade.gerarModeloInicial(aluno, ObjetivoPlano.HIPERTROFIA));
        assertThrows(AutorizacaoNegadaException.class,
                () -> facade.consultarMinhasNotificacoes(personal));
    }

    @Test
    void localizaSomenteAlunosPeloEmail() {
        assertEquals(aluno.usuarioId(),
                facade.localizarAlunoPorEmail(personal, "  " + EMAIL_ALUNO.toUpperCase()).id());
        assertThrows(RecursoNaoEncontradoException.class,
                () -> facade.localizarAlunoPorEmail(personal, EMAIL_PERSONAL));
    }

    @Test
    void substituirTreinoMantemPosicaoERejeitaNomeRepetidoSemPerderConteudo() {
        UUID plano = planoDemo();
        VersaoPlano rascunho = facade.iniciarRevisao(personal, plano);
        Treino primeiro = rascunho.getTreinos().get(0);

        assertThrows(ValorInvalidoException.class, () -> facade.substituirTreino(
                personal, plano, 2, "Treino A", new Treino("Treino B")));

        ItemTreino item = primeiro.getItens().get(0).comCarga(new Carga(new BigDecimal("55")));
        facade.substituirTreino(personal, plano, 2, "Treino A",
                new Treino("Treino A", List.of(item)));

        VersaoPlano salva = facade.consultarHistoricoVersoes(personal, plano).get(1);
        assertEquals("Treino A", salva.getTreinos().get(0).getNome());
        assertEquals(1, salva.getTreinos().get(0).getItens().size());
        assertEquals(2, salva.getTreinos().size());

        // Versão publicada não aceita essa operação.
        assertThrows(TransicaoInvalidaException.class, () -> facade.substituirTreino(
                personal, plano, 1, "Treino A", new Treino("Treino A", List.of(item))));
    }

    @Test
    void exercicioInativoContinuaConsultavel() {
        UUID exercicio = facade.listarExerciciosAtivos(personal).get(0).getId();
        facade.inativarExercicio(personal, exercicio);

        assertFalse(facade.consultarExercicio(aluno, exercicio).isAtivo());
    }
}
