
package br.ufpb.jwpersonal.application.facade;

import br.ufpb.jwpersonal.application.evento.PlanoAtribuidoEvento;
import br.ufpb.jwpersonal.application.service.AlunoResumo;
import br.ufpb.jwpersonal.application.service.AtribuicaoService;
import br.ufpb.jwpersonal.application.service.AutenticacaoService;
import br.ufpb.jwpersonal.application.service.CatalogoExerciciosService;
import br.ufpb.jwpersonal.application.service.ConsultaService;
import br.ufpb.jwpersonal.application.service.ExecucaoService;
import br.ufpb.jwpersonal.application.service.PrescricaoService;
import br.ufpb.jwpersonal.application.service.ResultadoRegistroExecucao;
import br.ufpb.jwpersonal.application.service.SessaoUsuario;

import br.ufpb.jwpersonal.domain.fabrica.ModeloInicialPlano;
import br.ufpb.jwpersonal.domain.model.Atribuicao;
import br.ufpb.jwpersonal.domain.model.Execucao;
import br.ufpb.jwpersonal.domain.model.Exercicio;
import br.ufpb.jwpersonal.domain.model.ObjetivoPlano;
import br.ufpb.jwpersonal.domain.model.PlanoTreino;
import br.ufpb.jwpersonal.domain.model.Treino;
import br.ufpb.jwpersonal.domain.model.VersaoPlano;

import br.ufpb.jwpersonal.domain.politica.PoliticaProgressao;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Objects;

/**
 * Fachada principal do JW Personal Trainer.
 *
 * Padrao GoF: Facade (Estrutural).
 *
 * Centraliza o acesso aos servicos da aplicacao,
 * simplificando a comunicacao com a interface.
 *
 * A interface nao precisa conhecer os servicos
 * individualmente nem acessar os repositorios.
 */
public class JWPersonalTrainerFacade {

    private final AutenticacaoService autenticacao;
    private final CatalogoExerciciosService catalogo;
    private final PrescricaoService prescricao;
    private final AtribuicaoService atribuicao;
    private final ExecucaoService execucao;
    private final ConsultaService consulta;

    public JWPersonalTrainerFacade(
            AutenticacaoService autenticacao,
            CatalogoExerciciosService catalogo,
            PrescricaoService prescricao,
            AtribuicaoService atribuicao,
            ExecucaoService execucao,
            ConsultaService consulta) {

        this.autenticacao = Objects.requireNonNull(autenticacao);
        this.catalogo = Objects.requireNonNull(catalogo);
        this.prescricao = Objects.requireNonNull(prescricao);
        this.atribuicao = Objects.requireNonNull(atribuicao);
        this.execucao = Objects.requireNonNull(execucao);
        this.consulta = Objects.requireNonNull(consulta);
    }

    // =====================================================
    // AUTENTICACAO
    // =====================================================

    public SessaoUsuario autenticar(String email, String senha) {
        return autenticacao.autenticar(email, senha);
    }

    // =====================================================
    // CATALOGO DE EXERCICIOS
    // =====================================================

    public Exercicio cadastrarExercicio(
            SessaoUsuario sessao,
            String nome,
            String instrucoes,
            String videoUrl,
            String fonte) {

        return catalogo.cadastrar(
                sessao, nome, instrucoes, videoUrl, fonte);
    }

    public Exercicio atualizarExercicio(
            SessaoUsuario sessao,
            UUID exercicioId,
            String instrucoes,
            String videoUrl,
            String fonte) {

        return catalogo.atualizarConteudo(
                sessao, exercicioId, instrucoes, videoUrl, fonte);
    }

    public void inativarExercicio(
            SessaoUsuario sessao,
            UUID exercicioId) {

        catalogo.inativar(sessao, exercicioId);
    }

    public List<Exercicio> listarExerciciosAtivos(
            SessaoUsuario sessao) {

        return catalogo.listarAtivos(sessao);
    }

    public Exercicio consultarExercicio(
            SessaoUsuario sessao,
            UUID exercicioId) {

        return catalogo.consultar(sessao, exercicioId);
    }

    // =====================================================
    // CU01 E CU02 - PRESCRICAO E REVISAO DE PLANOS
    // =====================================================

    public ModeloInicialPlano gerarModeloInicial(
            SessaoUsuario sessao,
            ObjetivoPlano objetivo) {

        return prescricao.gerarModeloInicial(sessao, objetivo);
    }

    public List<PlanoTreino> listarMeusPlanos(SessaoUsuario sessao) {
        return consulta.listarPlanosDoPersonal(sessao);
    }

    public String consultarTituloDoPlano(
            SessaoUsuario sessao,
            UUID planoId) {

        return consulta.consultarTituloDoPlano(sessao, planoId);
    }

    public PlanoTreino prescreverPlano(
            SessaoUsuario sessao,
            String titulo,
            ObjetivoPlano objetivo,
            List<Treino> treinos) {

        return prescricao.prescrever(
                sessao, titulo, objetivo, treinos);
    }

    public VersaoPlano iniciarRevisao(
            SessaoUsuario sessao,
            UUID planoId) {

        return prescricao.iniciarRevisao(sessao, planoId);
    }

    public void adicionarTreino(
            SessaoUsuario sessao,
            UUID planoId,
            int numeroVersao,
            Treino treino) {

        prescricao.adicionarTreino(
                sessao, planoId, numeroVersao, treino);
    }

    public void removerTreino(
            SessaoUsuario sessao,
            UUID planoId,
            int numeroVersao,
            String nomeTreino) {

        prescricao.removerTreino(
                sessao, planoId, numeroVersao, nomeTreino);
    }

    public void substituirTreino(
            SessaoUsuario sessao,
            UUID planoId,
            int numeroVersao,
            String nomeAtual,
            Treino novoTreino) {

        prescricao.substituirTreino(
                sessao, planoId, numeroVersao, nomeAtual, novoTreino);
    }

    public boolean podeCorrigirDiretamente(
            SessaoUsuario sessao,
            UUID planoId,
            int numeroVersao) {

        return prescricao.podeCorrigirDiretamente(
                sessao, planoId, numeroVersao);
    }

    public void aplicarProgressao(
            SessaoUsuario sessao,
            UUID planoId,
            int numeroVersao,
            PoliticaProgressao politica) {

        prescricao.aplicarProgressao(
                sessao, planoId, numeroVersao, politica);
    }

    public void publicarPlano(
            SessaoUsuario sessao,
            UUID planoId,
            int numeroVersao) {

        prescricao.publicar(
                sessao, planoId, numeroVersao);
    }

    public void corrigirVersao(
            SessaoUsuario sessao,
            UUID planoId,
            int numeroVersao,
            List<Treino> treinosCorrigidos) {

        prescricao.corrigirVersao(
                sessao, planoId, numeroVersao, treinosCorrigidos);
    }

    // =====================================================
    // CU03 - ATRIBUICAO DE PLANOS
    // =====================================================

    public Atribuicao atribuirPlano(
            SessaoUsuario sessao,
            UUID alunoId,
            UUID planoId,
            int numeroVersao) {

        return atribuicao.atribuir(
                sessao, alunoId, planoId, numeroVersao);
    }

    public AlunoResumo localizarAlunoPorEmail(
            SessaoUsuario sessao,
            String email) {

        return atribuicao.localizarAlunoPorEmail(sessao, email);
    }

    public List<Atribuicao> consultarAtribuicoesDoAluno(
            SessaoUsuario sessao,
            UUID alunoId) {

        return atribuicao.historicoParaPersonal(sessao, alunoId);
    }

    public Optional<Atribuicao> buscarMinhaAtribuicaoAtiva(
            SessaoUsuario sessao) {

        return atribuicao.buscarAtribuicaoAtiva(sessao);
    }

    public List<Atribuicao> consultarMeuHistoricoAtribuicoes(
            SessaoUsuario sessao) {

        return atribuicao.historicoDoAluno(sessao);
    }

    // =====================================================
    // CU04 - EXECUCAO DE TREINOS
    // =====================================================

    public ResultadoRegistroExecucao registrarExecucao(
            SessaoUsuario sessao,
            String nomeTreino,
            LocalDate data) {

        return execucao.registrarExecucao(
                sessao, nomeTreino, data);
    }

    // =====================================================
    // NOTIFICACOES (OBSERVER)
    // =====================================================

    public List<PlanoAtribuidoEvento> consultarMinhasNotificacoes(
            SessaoUsuario sessao) {

        return consulta.consultarMinhasNotificacoes(sessao);
    }

    // =====================================================
    // CONSULTAS
    // =====================================================

    public Optional<VersaoPlano> consultarPlanoAtivo(
            SessaoUsuario sessao,
            UUID alunoId) {

        return consulta.consultarPlanoAtivo(sessao, alunoId);
    }

    public Optional<VersaoPlano> consultarMeuPlanoAtivo(
            SessaoUsuario sessao) {

        return consulta.consultarPlanoAtivo(
                sessao, sessao.usuarioId());
    }

    public List<VersaoPlano> consultarHistoricoVersoes(
            SessaoUsuario sessao,
            UUID planoId) {

        return consulta.consultarHistoricoVersoes(
                sessao, planoId);
    }

    public List<Execucao> consultarHistoricoExecucoes(
            SessaoUsuario sessao,
            UUID alunoId) {

        return consulta.consultarHistoricoExecucoes(
                sessao, alunoId);
    }

    public List<Execucao> consultarMeuHistoricoExecucoes(
            SessaoUsuario sessao) {

        return consulta.consultarHistoricoExecucoes(
                sessao, sessao.usuarioId());
    }
}
