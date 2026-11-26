
package br.ufpb.jwpersonal.application.facade;

import br.ufpb.jwpersonal.application.evento.PublicadorDeEventos;
import br.ufpb.jwpersonal.application.service.*;
import br.ufpb.jwpersonal.domain.excecao.AutorizacaoNegadaException;
import br.ufpb.jwpersonal.domain.fabrica.GeradorDePlano;
import br.ufpb.jwpersonal.domain.fabrica.GeradorEmagrecimento;
import br.ufpb.jwpersonal.domain.fabrica.GeradorHipertrofia;
import br.ufpb.jwpersonal.domain.model.*;
import br.ufpb.jwpersonal.infra.memoria.*;
import br.ufpb.jwpersonal.infra.seguranca.Pbkdf2SenhaHasher;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JWPersonalTrainerFacadeTest {

    @Test
    void personalConsegueUtilizarCatalogoPelaFacade() {

        Ambiente ambiente = criarAmbiente();

        Usuario personal = new Usuario(
                "Personal Teste",
                "personal@teste.com",
                ambiente.hasher().gerarHash("senha123"),
                Perfil.PERSONAL
        );

        ambiente.usuarios().salvar(personal);

        SessaoUsuario sessao = ambiente.facade()
                .autenticar("personal@teste.com", "senha123");

        Exercicio cadastrado = ambiente.facade().cadastrarExercicio(
                sessao,
                "Agachamento",
                "Manter a postura correta durante o movimento.",
                null,
                "Material de demonstracao"
        );

        var exercicios = ambiente.facade()
                .listarExerciciosAtivos(sessao);

        assertNotNull(cadastrado);
        assertEquals("Agachamento", cadastrado.getNome());

        assertEquals(1, exercicios.size());
        assertEquals("Agachamento", exercicios.get(0).getNome());
    }

    @Test
    void alunoNaoPodeCadastrarExercicioPelaFacade() {

        Ambiente ambiente = criarAmbiente();

        Usuario aluno = new Usuario(
                "Aluno Teste",
                "aluno@teste.com",
                ambiente.hasher().gerarHash("senha123"),
                Perfil.ALUNO
        );

        ambiente.usuarios().salvar(aluno);

        SessaoUsuario sessao = ambiente.facade()
                .autenticar("aluno@teste.com", "senha123");

        assertThrows(
                AutorizacaoNegadaException.class,
                () -> ambiente.facade().cadastrarExercicio(
                        sessao,
                        "Supino",
                        "Descricao do exercicio",
                        null,
                        null
                )
        );
    }

    private Ambiente criarAmbiente() {

        // Repositorios em memoria
        var usuarios = new MemoriaUsuarioRepository();
        var exercicios = new MemoriaExercicioRepository();
        var planos = new MemoriaPlanoRepository();
        var atribuicoes = new MemoriaAtribuicaoRepository();
        var execucoes = new MemoriaExecucaoRepository();

        // Unidade de trabalho
        var unidadeDeTrabalho =
                new MemoriaUnidadeDeTrabalho(atribuicoes);

        var autorizador = new Autorizador();

        // Hash de senhas
        var hasher = new Pbkdf2SenhaHasher();

        // Registro dos geradores do padrao Factory Method
        Map<ObjetivoPlano, GeradorDePlano> geradores =
                new EnumMap<>(ObjetivoPlano.class);

        geradores.put(
                ObjetivoPlano.HIPERTROFIA,
                new GeradorHipertrofia()
        );

        geradores.put(
                ObjetivoPlano.EMAGRECIMENTO,
                new GeradorEmagrecimento()
        );

        // Servico de autenticacao
        var autenticacao = new AutenticacaoService(
                usuarios,
                hasher
        );

        // Servico de catalogo
        var catalogo = new CatalogoExerciciosService(
                exercicios,
                autorizador
        );

        // Servico de prescricao com Factory Method
        var prescricao = new PrescricaoService(
                planos,
                exercicios,
                execucoes,
                unidadeDeTrabalho,
                autorizador,
                geradores
        );

        // Publicador do padrao Observer
        var publicador = new PublicadorDeEventos();

        // Servico de atribuicao com Observer
        var atribuicaoService = new AtribuicaoService(
                atribuicoes,
                planos,
                usuarios,
                unidadeDeTrabalho,
                autorizador,
                publicador
        );

        // Servico de execucao
        var execucaoService = new ExecucaoService(
                execucoes,
                atribuicoes,
                planos,
                unidadeDeTrabalho,
                autorizador
        );

        // Servico de consultas
        var consulta = new ConsultaService(
                planos,
                atribuicoes,
                execucoes,
                autorizador
        );

        // Criacao da Facade
        var facade = new JWPersonalTrainerFacade(
                autenticacao,
                catalogo,
                prescricao,
                atribuicaoService,
                execucaoService,
                consulta
        );

        return new Ambiente(
                facade,
                usuarios,
                hasher
        );
    }

    // Ambiente de testes
    private record Ambiente(
            JWPersonalTrainerFacade facade,
            MemoriaUsuarioRepository usuarios,
            Pbkdf2SenhaHasher hasher
    ) {}
}
