
package br.ufpb.jwpersonal.application.service;

import br.ufpb.jwpersonal.application.evento.PlanoAtribuidoEvento;
import br.ufpb.jwpersonal.application.evento.PublicadorDeEventos;
import br.ufpb.jwpersonal.domain.excecao.RecursoNaoEncontradoException;
import br.ufpb.jwpersonal.domain.excecao.ValorInvalidoException;
import br.ufpb.jwpersonal.domain.model.*;
import br.ufpb.jwpersonal.repository.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Responsável por atribuir versões de planos aos alunos.
 *
 * CU03 - Atribuir plano a aluno.
 * RN01 - Autorização.
 * RN05 - Uma única atribuição ativa por aluno.
 *
 * Utiliza Observer para notificar o aluno após
 * a confirmação de uma nova atribuição.
 */
public class AtribuicaoService {

    private final AtribuicaoRepository atribuicoes;
    private final PlanoRepository planos;
    private final UsuarioRepository usuarios;
    private final UnidadeDeTrabalho unidadeDeTrabalho;
    private final Autorizador autorizador;
    private final PublicadorDeEventos publicador;

    /**
     * Construtor compatível com os componentes existentes.
     *
     * Quando um publicador não é fornecido, cria um publicador
     * sem observadores inscritos.
     */
    public AtribuicaoService(
            AtribuicaoRepository atribuicoes,
            PlanoRepository planos,
            UsuarioRepository usuarios,
            UnidadeDeTrabalho unidadeDeTrabalho,
            Autorizador autorizador) {

        this(
                atribuicoes,
                planos,
                usuarios,
                unidadeDeTrabalho,
                autorizador,
                new PublicadorDeEventos()
        );
    }

    /**
     * Construtor com injeção do publicador de eventos.
     */
    public AtribuicaoService(
            AtribuicaoRepository atribuicoes,
            PlanoRepository planos,
            UsuarioRepository usuarios,
            UnidadeDeTrabalho unidadeDeTrabalho,
            Autorizador autorizador,
            PublicadorDeEventos publicador) {

        this.atribuicoes = Objects.requireNonNull(atribuicoes);
        this.planos = Objects.requireNonNull(planos);
        this.usuarios = Objects.requireNonNull(usuarios);
        this.unidadeDeTrabalho =
                Objects.requireNonNull(unidadeDeTrabalho);
        this.autorizador = Objects.requireNonNull(autorizador);
        this.publicador = Objects.requireNonNull(publicador);
    }

    /**
     * CU03: atribui uma versão publicada a um aluno.
     *
     * Se existir outra atribuição ativa, ela será encerrada
     * na mesma transação.
     *
     * Repetir a mesma atribuição não cria duplicata
     * nem gera uma nova notificação.
     *
     * O evento só é publicado depois que a unidade
     * de trabalho termina com sucesso.
     */
    public Atribuicao atribuir(
            SessaoUsuario sessao,
            UUID alunoId,
            UUID planoId,
            int numeroVersao) {

        autorizador.exigirPersonal(sessao);

        Objects.requireNonNull(
                alunoId,
                "O ID do aluno é obrigatório."
        );

        Objects.requireNonNull(
                planoId,
                "O ID do plano é obrigatório."
        );

        // A operação retorna o resultado somente depois
        // da conclusão da unidade de trabalho.
        ResultadoAtribuicao resultado =
                unidadeDeTrabalho.executar(() -> {

                    // 1. Verificar se o aluno existe.
                    Usuario aluno = usuarios.buscarPorId(alunoId)
                            .orElseThrow(() ->
                                    new RecursoNaoEncontradoException(
                                            "Aluno não encontrado."
                                    )
                            );

                    // 2. Verificar o perfil e a situação.
                    if (!aluno.possuiPerfil(Perfil.ALUNO)
                            || !aluno.isAtivo()) {

                        throw new ValorInvalidoException(
                                "O usuário deve ser um aluno ativo."
                        );
                    }

                    // 3. Buscar o plano.
                    PlanoTreino plano = planos.buscarPorId(planoId)
                            .orElseThrow(() ->
                                    new RecursoNaoEncontradoException(
                                            "Plano não encontrado."
                                    )
                            );

                    // 4. Conferir se o Personal é o autor.
                    autorizador.exigirPersonalDoPlano(
                            sessao,
                            plano
                    );

                    // 5. Buscar a versão solicitada.
                    VersaoPlano versao =
                            plano.versaoPorNumero(numeroVersao);

                    // 6. Buscar atribuição atual.
                    Optional<Atribuicao> atual =
                            atribuicoes.buscarAtivaDoAluno(alunoId);

                    // 7. Impedir atribuição duplicada.
                    if (atual.isPresent()) {

                        Atribuicao existente = atual.get();

                        if (existente.getPlanoId().equals(planoId)
                                && existente.getNumeroVersao()
                                == numeroVersao) {

                            // Não deve publicar um novo evento.
                            return new ResultadoAtribuicao(
                                    existente,
                                    false,
                                    plano.getTitulo()
                            );
                        }
                    }

                    // 8. Permitir apenas versão publicada.
                    if (versao.getEstado() != EstadoVersao.PUBLICADA) {

                        throw new ValorInvalidoException(
                                "Somente versões publicadas "
                                        + "podem receber novas atribuições."
                        );
                    }

                    LocalDate hoje = LocalDate.now();

                    // 9. Encerrar atribuição anterior.
                    if (atual.isPresent()) {

                        Atribuicao anterior = atual.get();

                        anterior.encerrar(hoje);

                        atribuicoes.salvar(anterior);
                    }

                    // 10. Criar nova atribuição.
                    Atribuicao nova = new Atribuicao(
                            alunoId,
                            planoId,
                            numeroVersao,
                            hoje
                    );

                    // 11. Salvar nova atribuição.
                    atribuicoes.salvar(nova);

                    return new ResultadoAtribuicao(
                            nova,
                            true,
                            plano.getTitulo()
                    );
                });

        /*
         * IMPORTANTE:
         * Este trecho está fora de unidadeDeTrabalho.executar().
         *
         * Portanto, quando chegar aqui, a operação já terminou
         * e, no SQLite, o commit já foi realizado.
         *
         * Apenas uma nova atribuição gera o evento.
         */
        if (resultado.novaCriada()) {

            Atribuicao nova = resultado.atribuicao();

            PlanoAtribuidoEvento evento =
                    new PlanoAtribuidoEvento(
                            nova.getAlunoId(),
                            nova.getPlanoId(),
                            nova.getNumeroVersao(),
                            resultado.tituloPlano(),
                            nova.getInicio()
                    );

            publicador.publicar(evento);
        }

        return resultado.atribuicao();
    }

    /**
     * Permite ao Personal identificar um aluno pelo e-mail exato.
     * A mensagem é a mesma para e-mail inexistente, de outro perfil ou inativo.
     */
    public AlunoResumo localizarAlunoPorEmail(
            SessaoUsuario sessao,
            String email) {

        autorizador.exigirPersonal(sessao);

        if (email == null || email.isBlank()) {
            throw new ValorInvalidoException("O e-mail é obrigatório.");
        }

        Usuario aluno = usuarios
                .buscarPorEmail(email.trim().toLowerCase(Locale.ROOT))
                .filter(u -> u.possuiPerfil(Perfil.ALUNO) && u.isAtivo())
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "Aluno não encontrado."));

        return new AlunoResumo(
                aluno.getId(), aluno.getNome(), aluno.getEmail());
    }

    /**
     * Histórico de atribuições de um aluno, restrito aos planos
     * de autoria do Personal autenticado (RN01).
     */
    public List<Atribuicao> historicoParaPersonal(
            SessaoUsuario sessao,
            UUID alunoId) {

        autorizador.exigirPersonal(sessao);

        Objects.requireNonNull(alunoId, "O ID do aluno é obrigatório.");

        return atribuicoes.historicoDoAluno(alunoId)
                .stream()
                .filter(a -> planos.buscarPorId(a.getPlanoId())
                        .map(p -> sessao.pertenceA(p.getPersonalId()))
                        .orElse(false))
                .toList();
    }

    /**
     * Busca a atribuição ativa do próprio aluno.
     */
    public Optional<Atribuicao> buscarAtribuicaoAtiva(
            SessaoUsuario sessao) {

        autorizador.exigirAluno(sessao);

        return atribuicoes.buscarAtivaDoAluno(
                sessao.usuarioId()
        );
    }

    /**
     * Consulta o histórico de atribuições do próprio aluno.
     */
    public List<Atribuicao> historicoDoAluno(
            SessaoUsuario sessao) {

        autorizador.exigirAluno(sessao);

        return atribuicoes.historicoDoAluno(
                sessao.usuarioId()
        );
    }

    /**
     * Resultado interno da operação de atribuição.
     */
    private record ResultadoAtribuicao(
            Atribuicao atribuicao,
            boolean novaCriada,
            String tituloPlano
    ) {
    }
}
