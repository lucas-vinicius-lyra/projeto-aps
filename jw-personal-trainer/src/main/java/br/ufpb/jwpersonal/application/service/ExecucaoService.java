
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
 * Responsável pelo registro e consulta das execuções.
 *
 * CU04 - Registrar execução de treino.
 * RN01 - Autorização.
 * RN06 - Execução válida e idempotente.
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
     * CU04: registra a conclusão de um treino.
     *
     * Só permite registro na atribuição ativa do aluno.
     * Se a execução já existir, devolve o registro anterior
     * e informa que não foi criado um novo.
     */
    public ResultadoRegistroExecucao registrarExecucao(
            SessaoUsuario sessao,
            String nomeTreino,
            LocalDate data) {

        autorizador.exigirAluno(sessao);

        if (nomeTreino == null || nomeTreino.isBlank()) {
            throw new ValorInvalidoException(
                    "O nome do treino é obrigatório."
            );
        }

        Objects.requireNonNull(
                data,
                "A data é obrigatória."
        );

        return unidadeDeTrabalho.executar(() -> {

            UUID alunoId = sessao.usuarioId();

            // 1. Buscar a atribuição ativa do aluno.
            Atribuicao atribuicao =
                    atribuicoes.buscarAtivaDoAluno(alunoId)
                            .orElseThrow(() ->
                                    new RecursoNaoEncontradoException(
                                            "O aluno não possui atribuição ativa."
                                    )
                            );

            // 2. Conferir se a atribuição está ativa.
            if (!atribuicao.estaAtiva()) {
                throw new ValorInvalidoException(
                        "A atribuição não está ativa."
                );
            }

            // 3. Validar a data da execução.
            if (data.isAfter(LocalDate.now())
                    || data.isBefore(atribuicao.getInicio())) {

                throw new ValorInvalidoException(
                        "Data fora do período permitido."
                );
            }

            // 4. Buscar o plano atribuído.
            PlanoTreino plano =
                    planos.buscarPorId(atribuicao.getPlanoId())
                            .orElseThrow(() ->
                                    new RecursoNaoEncontradoException(
                                            "Plano não encontrado."
                                    )
                            );

            // 5. Buscar a versão exata da atribuição.
            VersaoPlano versao =
                    plano.versaoPorNumero(
                            atribuicao.getNumeroVersao()
                    );

            // 6. Verificar se o treino pertence à versão.
            Treino treino = versao.getTreinos().stream()
                    .filter(t -> t.getNome()
                            .equalsIgnoreCase(nomeTreino.trim()))
                    .findFirst()
                    .orElseThrow(() ->
                            new RecursoNaoEncontradoException(
                                    "Treino não pertence à versão atribuída."
                            )
                    );

            // 7. Preparar o novo registro.
            Execucao nova = new Execucao(
                    alunoId,
                    atribuicao.getId(),
                    atribuicao.getPlanoId(),
                    atribuicao.getNumeroVersao(),
                    treino.getNome(),
                    data
            );

            // 8. Verificar se a execução já existe.
            Optional<Execucao> existente =
                    buscarExecucaoExistente(
                            alunoId,
                            nova.chaveNatural()
                    );

            // 9. Se existir, devolver o registro anterior.
            if (existente.isPresent()) {
                return new ResultadoRegistroExecucao(
                        existente.get(),
                        true
                );
            }

            // 10. Se não existir, salvar o novo registro.
            execucoes.salvar(nova);

            return new ResultadoRegistroExecucao(
                    nova,
                    false
            );
        });
    }

    /**
     * Consulta o histórico de execuções do próprio aluno.
     */
    public List<Execucao> consultarHistorico(
            SessaoUsuario sessao) {

        autorizador.exigirAluno(sessao);

        return List.copyOf(
                execucoes.historicoDoAluno(
                        sessao.usuarioId()
                )
        );
    }

    /**
     * Busca uma execução pela chave natural.
     *
     * Utiliza o histórico já disponível no contrato
     * ExecucaoRepository.
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
