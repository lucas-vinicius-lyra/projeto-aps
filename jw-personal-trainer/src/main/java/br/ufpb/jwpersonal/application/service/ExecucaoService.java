
package br.ufpb.jwpersonal.application.service;

import br.ufpb.jwpersonal.domain.excecao.RecursoNaoEncontradoException;
import br.ufpb.jwpersonal.domain.excecao.ValorInvalidoException;
import br.ufpb.jwpersonal.domain.model.*;
import br.ufpb.jwpersonal.repository.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Responsavel pelo registro e consulta das execucoes.
 *
 * CU04 - Registrar execucao de treino.
 * RN01 - Autorizacao.
 * RN06 - Execucao valida e idempotente.
 */
public class ExecucaoService {

    private final ExecucaoRepository execucoes;
    private final AtribuicaoRepository atribuicoes;
    private final PlanoRepository planos;
    private final UnidadeDeTrabalho unidadeDeTrabalho;
    private final Autorizador autorizador;

    public ExecucaoService(
            ExecucaoRepository execucoes,
            AtribuicaoRepository atribuicoes,
            PlanoRepository planos,
            UnidadeDeTrabalho unidadeDeTrabalho,
            Autorizador autorizador) {

        this.execucoes = Objects.requireNonNull(execucoes);
        this.atribuicoes = Objects.requireNonNull(atribuicoes);
        this.planos = Objects.requireNonNull(planos);
        this.unidadeDeTrabalho =
                Objects.requireNonNull(unidadeDeTrabalho);
        this.autorizador = Objects.requireNonNull(autorizador);
    }

    /**
     * CU04: registra a conclusao de um treino.
     *
     * So permite registro na atribuicao ativa do aluno.
     * Repetir a mesma execucao devolve o registro existente.
     */
    public Execucao registrarExecucao(
            SessaoUsuario sessao,
            String nomeTreino,
            LocalDate data) {

        autorizador.exigirAluno(sessao);

        if (nomeTreino == null || nomeTreino.isBlank()) {
            throw new ValorInvalidoException(
                    "O nome do treino e obrigatorio.");
        }

        Objects.requireNonNull(
                data, "A data e obrigatoria.");

        return unidadeDeTrabalho.executar(() -> {

            UUID alunoId = sessao.usuarioId();

            // 1. Buscar a atribuicao ativa do aluno
            Atribuicao atribuicao =
                    atribuicoes.buscarAtivaDoAluno(alunoId)
                    .orElseThrow(() ->
                            new RecursoNaoEncontradoException(
                                    "O aluno nao possui atribuicao ativa."));

            // 2. Conferir se a atribuicao esta ativa
            if (!atribuicao.estaAtiva()) {
                throw new ValorInvalidoException(
                        "A atribuicao nao esta ativa.");
            }

            // 3. Validar a data da execucao
            if (data.isAfter(LocalDate.now())
                    || data.isBefore(atribuicao.getInicio())) {

                throw new ValorInvalidoException(
                        "Data fora do periodo permitido.");
            }

            // 4. Buscar o plano atribuido
            PlanoTreino plano =
                    planos.buscarPorId(atribuicao.getPlanoId())
                    .orElseThrow(() ->
                            new RecursoNaoEncontradoException(
                                    "Plano nao encontrado."));

            // 5. Buscar a versao exata da atribuicao
            VersaoPlano versao =
                    plano.versaoPorNumero(
                            atribuicao.getNumeroVersao());

            // 6. Verificar se o treino pertence a versao
            Treino treino = versao.getTreinos().stream()
                    .filter(t -> t.getNome()
                            .equalsIgnoreCase(nomeTreino.trim()))
                    .findFirst()
                    .orElseThrow(() ->
                            new RecursoNaoEncontradoException(
                                    "Treino nao pertence a versao atribuida."));

            // 7. Preparar o registro de execucao
            Execucao nova = new Execucao(
                    alunoId,
                    atribuicao.getId(),
                    atribuicao.getPlanoId(),
                    atribuicao.getNumeroVersao(),
                    treino.getNome(),
                    data
            );

            // 8. Verificar duplicidade pela chave natural
            return buscarExecucaoExistente(
                    alunoId, nova.chaveNatural())
                    .orElseGet(() -> {

                        // 9. Salvar somente se ainda nao existir
                        execucoes.salvar(nova);

                        return nova;
                    });
        });
    }

    /**
     * Consulta o historico de execucoes do proprio aluno.
     */
    public List<Execucao> consultarHistorico(
            SessaoUsuario sessao) {

        autorizador.exigirAluno(sessao);

        return List.copyOf(
                execucoes.historicoDoAluno(
                        sessao.usuarioId()));
    }

    /**
     * Busca uma execucao pela chave natural.
     *
     * Utiliza o metodo historicoDoAluno,
     * ja existente no contrato ExecucaoRepository.
     */
    private Optional<Execucao> buscarExecucaoExistente(
            UUID alunoId,
            String chaveNatural) {

        return execucoes.historicoDoAluno(alunoId)
                .stream()
                .filter(e -> e.chaveNatural()
                        .equals(chaveNatural))
                .findFirst();
    }
}
