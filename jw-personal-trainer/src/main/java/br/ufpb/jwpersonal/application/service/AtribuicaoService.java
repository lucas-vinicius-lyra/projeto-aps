
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
 * Responsavel por atribuir versoes de planos aos alunos.
 *
 * CU03 - Atribuir plano a aluno.
 * RN01 - Autorizacao.
 * RN05 - Uma unica atribuicao ativa por aluno.
 */
public class AtribuicaoService {

    private final AtribuicaoRepository atribuicoes;
    private final PlanoRepository planos;
    private final UsuarioRepository usuarios;
    private final UnidadeDeTrabalho unidadeDeTrabalho;
    private final Autorizador autorizador;

    public AtribuicaoService(
            AtribuicaoRepository atribuicoes,
            PlanoRepository planos,
            UsuarioRepository usuarios,
            UnidadeDeTrabalho unidadeDeTrabalho,
            Autorizador autorizador) {

        this.atribuicoes = Objects.requireNonNull(atribuicoes);
        this.planos = Objects.requireNonNull(planos);
        this.usuarios = Objects.requireNonNull(usuarios);
        this.unidadeDeTrabalho =
                Objects.requireNonNull(unidadeDeTrabalho);
        this.autorizador = Objects.requireNonNull(autorizador);
    }

    /**
     * CU03: atribui uma versao publicada a um aluno.
     *
     * Se existir outra atribuicao ativa, ela sera
     * encerrada na mesma transacao.
     *
     * Repetir a mesma atribuicao nao cria duplicata.
     */
    public Atribuicao atribuir(
            SessaoUsuario sessao,
            UUID alunoId,
            UUID planoId,
            int numeroVersao) {

        autorizador.exigirPersonal(sessao);

        Objects.requireNonNull(alunoId,
                "O ID do aluno e obrigatorio.");

        Objects.requireNonNull(planoId,
                "O ID do plano e obrigatorio.");

        return unidadeDeTrabalho.executar(() -> {

            // 1. Verificar se o aluno existe
            Usuario aluno = usuarios.buscarPorId(alunoId)
                    .orElseThrow(() ->
                            new RecursoNaoEncontradoException(
                                    "Aluno nao encontrado."));

            // 2. Verificar o perfil e a situacao
            if (!aluno.possuiPerfil(Perfil.ALUNO)
                    || !aluno.isAtivo()) {

                throw new ValorInvalidoException(
                        "O usuario deve ser um aluno ativo.");
            }

            // 3. Buscar o plano
            PlanoTreino plano = planos.buscarPorId(planoId)
                    .orElseThrow(() ->
                            new RecursoNaoEncontradoException(
                                    "Plano nao encontrado."));

            // 4. Conferir se o personal e o autor
            autorizador.exigirPersonalDoPlano(
                    sessao, plano);

            // 5. Buscar a versao solicitada
            VersaoPlano versao =
                    plano.versaoPorNumero(numeroVersao);

            // 6. Verificar atribuicao atual
            Optional<Atribuicao> atual =
                    atribuicoes.buscarAtivaDoAluno(alunoId);

            // 7. Impedir atribuicao duplicada
            if (atual.isPresent()) {

                Atribuicao existente = atual.get();

                if (existente.getPlanoId().equals(planoId)
                        && existente.getNumeroVersao()
                                == numeroVersao) {

                    return existente;
                }
            }

            // 8. Permitir apenas versao publicada
            if (versao.getEstado()
                    != EstadoVersao.PUBLICADA) {

                throw new ValorInvalidoException(
                        "Somente versoes publicadas "
                        + "podem receber novas atribuicoes.");
            }

            LocalDate hoje = LocalDate.now();

            // 9. Encerrar atribuicao anterior
            if (atual.isPresent()) {

                Atribuicao anterior = atual.get();

                anterior.encerrar(hoje);

                atribuicoes.salvar(anterior);
            }

            // 10. Criar nova atribuicao
            Atribuicao nova = new Atribuicao(
                    alunoId,
                    planoId,
                    numeroVersao,
                    hoje
            );

            // 11. Salvar nova atribuicao
            atribuicoes.salvar(nova);

            return nova;
        });
    }

    /**
     * Busca a atribuicao ativa do proprio aluno.
     */
    public Optional<Atribuicao> buscarAtribuicaoAtiva(
            SessaoUsuario sessao) {

        autorizador.exigirAluno(sessao);

        return atribuicoes.buscarAtivaDoAluno(
                sessao.usuarioId());
    }

    /**
     * Consulta o historico de atribuicoes do proprio aluno.
     */
    public List<Atribuicao> historicoDoAluno(
            SessaoUsuario sessao) {

        autorizador.exigirAluno(sessao);

        return atribuicoes.historicoDoAluno(
                sessao.usuarioId());
    }
}
