
package br.ufpb.jwpersonal.application.service;

import br.ufpb.jwpersonal.domain.excecao.AutorizacaoNegadaException;
import br.ufpb.jwpersonal.domain.excecao.RecursoNaoEncontradoException;
import br.ufpb.jwpersonal.domain.model.Atribuicao;
import br.ufpb.jwpersonal.domain.model.Execucao;
import br.ufpb.jwpersonal.domain.model.EstadoVersao;
import br.ufpb.jwpersonal.domain.model.PlanoTreino;
import br.ufpb.jwpersonal.domain.model.VersaoPlano;
import br.ufpb.jwpersonal.repository.AtribuicaoRepository;
import br.ufpb.jwpersonal.repository.ExecucaoRepository;
import br.ufpb.jwpersonal.repository.PlanoRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Consultas de planos e historicos.
 *
 * RN01 - Autorizacao de acesso aos dados.
 */
public class ConsultaService {

    private final PlanoRepository planos;
    private final AtribuicaoRepository atribuicoes;
    private final ExecucaoRepository execucoes;
    private final Autorizador autorizador;

    public ConsultaService(
            PlanoRepository planos,
            AtribuicaoRepository atribuicoes,
            ExecucaoRepository execucoes,
            Autorizador autorizador) {

        this.planos = Objects.requireNonNull(planos);
        this.atribuicoes = Objects.requireNonNull(atribuicoes);
        this.execucoes = Objects.requireNonNull(execucoes);
        this.autorizador = Objects.requireNonNull(autorizador);
    }

    /**
     * Consulta a versao do plano ativo do aluno.
     */
    public Optional<VersaoPlano> consultarPlanoAtivo(
            SessaoUsuario sessao,
            UUID alunoId) {

        autorizador.exigirAutenticado(sessao);

        Objects.requireNonNull(
                alunoId, "O ID do aluno e obrigatorio.");

        Optional<Atribuicao> ativa =
                atribuicoes.buscarAtivaDoAluno(alunoId);

        // Aluno so acessa o proprio plano.
        if (sessao.ehAluno()) {

            autorizador.exigirProprioAluno(
                    sessao, alunoId);

        } else {

            // Personal deve ser responsavel pelo plano ativo.
            if (ativa.isEmpty()) {
                throw new AutorizacaoNegadaException(
                        "Nao ha atribuicao ativa que comprove "
                        + "o vinculo com este personal.");
            }

            verificarAutoria(
                    sessao, ativa.get().getPlanoId());
        }

        return ativa.map(a ->
                buscarPlano(a.getPlanoId())
                        .versaoPorNumero(
                                a.getNumeroVersao()));
    }

    /**
     * Consulta as versoes de um plano.
     *
     * Personal: apenas planos de sua autoria.
     * Aluno: apenas planos aos quais foi vinculado.
     */
    public List<VersaoPlano> consultarHistoricoVersoes(
            SessaoUsuario sessao,
            UUID planoId) {

        autorizador.exigirAutenticado(sessao);

        Objects.requireNonNull(
                planoId, "O ID do plano e obrigatorio.");

        PlanoTreino plano = buscarPlano(planoId);

        if (sessao.ehPersonal()) {

            autorizador.exigirPersonalDoPlano(
                    sessao, plano);

        } else {

            autorizador.exigirAluno(sessao);

            boolean vinculado =
                    atribuicoes.historicoDoAluno(
                            sessao.usuarioId())
                    .stream()
                    .anyMatch(a ->
                            a.getPlanoId().equals(planoId));

            if (!vinculado) {
                throw new AutorizacaoNegadaException(
                        "O aluno nao possui vinculo "
                        + "com este plano.");
            }
        }

        // Rascunhos nao devem aparecer para alunos.
        if (sessao.ehAluno()) {

            return plano.historico().stream()
                    .filter(v -> v.getEstado()
                            != EstadoVersao.RASCUNHO)
                    .toList();
        }

        return List.copyOf(plano.historico());
    }

    /**
     * Consulta o historico de execucoes de um aluno.
     *
     * O aluno acessa apenas seus registros.
     * O personal acessa execucoes dos planos
     * de sua responsabilidade.
     */
    public List<Execucao> consultarHistoricoExecucoes(
            SessaoUsuario sessao,
            UUID alunoId) {

        autorizador.exigirAutenticado(sessao);

        Objects.requireNonNull(
                alunoId, "O ID do aluno e obrigatorio.");

        if (sessao.ehAluno()) {

            autorizador.exigirProprioAluno(
                    sessao, alunoId);

            return List.copyOf(
                    execucoes.historicoDoAluno(alunoId));
        }

        autorizador.exigirPersonal(sessao);

        // Confirma vinculo historico entre personal e aluno.
        boolean vinculado =
                atribuicoes.historicoDoAluno(alunoId)
                .stream()
                .anyMatch(a ->
                        planos.buscarPorId(a.getPlanoId())
                        .map(p -> p.getPersonalId()
                                .equals(sessao.usuarioId()))
                        .orElse(false));

        if (!vinculado) {
            throw new AutorizacaoNegadaException(
                    "O aluno nao possui vinculo "
                    + "com este personal.");
        }

        // Retorna apenas execucoes dos planos
        // pertencentes ao personal autenticado.
        return execucoes.historicoDoAluno(alunoId)
                .stream()
                .filter(e ->
                        planos.buscarPorId(e.getPlanoId())
                        .map(p -> p.getPersonalId()
                                .equals(sessao.usuarioId()))
                        .orElse(false))
                .toList();
    }

    /**
     * Verifica se o personal e autor do plano.
     */
    private void verificarAutoria(
            SessaoUsuario sessao,
            UUID planoId) {

        autorizador.exigirPersonalDoPlano(
                sessao, buscarPlano(planoId));
    }

    /**
     * Busca um plano ou informa que nao existe.
     */
    private PlanoTreino buscarPlano(UUID planoId) {

        return planos.buscarPorId(planoId)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "Plano nao encontrado."));
    }
}
