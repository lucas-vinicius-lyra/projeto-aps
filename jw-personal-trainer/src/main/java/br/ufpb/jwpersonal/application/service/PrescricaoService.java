
package br.ufpb.jwpersonal.application.service;

import br.ufpb.jwpersonal.domain.excecao.RecursoNaoEncontradoException;
import br.ufpb.jwpersonal.domain.excecao.ValorInvalidoException;
import br.ufpb.jwpersonal.domain.model.*;
import br.ufpb.jwpersonal.domain.politica.PoliticaProgressao;
import br.ufpb.jwpersonal.repository.*;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Coordena os casos de uso de prescricao e revisao (CU01 e CU02). */
public class PrescricaoService {

    private final PlanoRepository planos;
    private final ExercicioRepository exercicios;
    private final ExecucaoRepository execucoes;
    private final UnidadeDeTrabalho unidadeDeTrabalho;
    private final Autorizador autorizador;

    public PrescricaoService(PlanoRepository planos,
                            ExercicioRepository exercicios,
                            ExecucaoRepository execucoes,
                            UnidadeDeTrabalho unidadeDeTrabalho,
                            Autorizador autorizador) {
        this.planos = Objects.requireNonNull(planos);
        this.exercicios = Objects.requireNonNull(exercicios);
        this.execucoes = Objects.requireNonNull(execucoes);
        this.unidadeDeTrabalho = Objects.requireNonNull(unidadeDeTrabalho);
        this.autorizador = Objects.requireNonNull(autorizador);
    }

    /** CU01: cria o plano com a versao 1 em RASCUNHO. */
    public PlanoTreino prescrever(SessaoUsuario sessao,
                                 String titulo,
                                 ObjetivoPlano objetivo,
                                 List<Treino> treinos) {
        autorizador.exigirPersonal(sessao);
        Objects.requireNonNull(treinos, "A lista de treinos e obrigatoria.");

        PlanoTreino plano = new PlanoTreino(titulo, objetivo, sessao.usuarioId());
        VersaoPlano rascunho = plano.versaoPorNumero(1);

        for (Treino treino : treinos) {
            validarExerciciosAtivos(treino.getItens());
            rascunho.adicionarTreino(treino);
        }

        return unidadeDeTrabalho.executar(() -> {
            planos.salvar(plano);
            return plano;
        });
    }

    /** CU02: copia a versao publicada para o proximo rascunho. */
    public VersaoPlano iniciarRevisao(SessaoUsuario sessao, UUID planoId) {
        PlanoTreino plano = buscarPlanoAutorizado(sessao, planoId);

        return unidadeDeTrabalho.executar(() -> {
            VersaoPlano revisao = plano.iniciarRevisao();
            planos.salvar(plano);
            return revisao;
        });
    }

    /** Adiciona uma sessao a um rascunho. */
    public void adicionarTreino(SessaoUsuario sessao, UUID planoId,
                               int numeroVersao, Treino treino) {
        PlanoTreino plano = buscarPlanoAutorizado(sessao, planoId);
        Objects.requireNonNull(treino, "O treino e obrigatorio.");
        validarExerciciosAtivos(treino.getItens());

        unidadeDeTrabalho.executar(() -> {
            plano.versaoPorNumero(numeroVersao).adicionarTreino(treino);
            planos.salvar(plano);
            return null;
        });
    }

    /** Remove uma sessao do rascunho para permitir substitui-la. */
    public void removerTreino(SessaoUsuario sessao, UUID planoId,
                             int numeroVersao, String nomeTreino) {
        PlanoTreino plano = buscarPlanoAutorizado(sessao, planoId);

        unidadeDeTrabalho.executar(() -> {
            plano.versaoPorNumero(numeroVersao).removerTreino(nomeTreino);
            planos.salvar(plano);
            return null;
        });
    }

    /** CU02 / RN08: aplica uma politica Strategy ao rascunho. */
    public void aplicarProgressao(SessaoUsuario sessao, UUID planoId,
                                 int numeroVersao, PoliticaProgressao politica) {
        PlanoTreino plano = buscarPlanoAutorizado(sessao, planoId);
        Objects.requireNonNull(politica, "A politica e obrigatoria.");

        unidadeDeTrabalho.executar(() -> {
            VersaoPlano versao = plano.versaoPorNumero(numeroVersao);
            versao.aplicarProgressao(politica);
            planos.salvar(plano);
            return null;
        });
    }

    /** RN02/RN09: publica o rascunho e arquiva a versao anterior. */
    public void publicar(SessaoUsuario sessao, UUID planoId, int numeroVersao) {
        PlanoTreino plano = buscarPlanoAutorizado(sessao, planoId);
        VersaoPlano versao = plano.versaoPorNumero(numeroVersao);

        versao.validarParaPublicacao();
        validarVersaoComExerciciosAtivos(versao);

        unidadeDeTrabalho.executar(() -> {
            plano.publicar(numeroVersao);
            planos.salvar(plano);
            return null;
        });
    }

    /** RN04: so permite corrigir versao publicada sem execucoes. */
    public void corrigirVersao(SessaoUsuario sessao, UUID planoId,
                              int numeroVersao, List<Treino> treinosCorrigidos) {
        PlanoTreino plano = buscarPlanoAutorizado(sessao, planoId);
        Objects.requireNonNull(treinosCorrigidos, "Os treinos sao obrigatorios.");

        for (Treino treino : treinosCorrigidos) {
            Objects.requireNonNull(treino, "Um treino nao pode ser nulo.");
            validarExerciciosAtivos(treino.getItens());
        }

        unidadeDeTrabalho.executar(() -> {
            boolean possuiExecucao =
                    execucoes.existeParaVersao(planoId, numeroVersao);

            plano.versaoPorNumero(numeroVersao)
                    .corrigir(treinosCorrigidos, possuiExecucao);

            planos.salvar(plano);
            return null;
        });
    }

    private PlanoTreino buscarPlanoAutorizado(
            SessaoUsuario sessao, UUID planoId) {
        autorizador.exigirPersonal(sessao);
        Objects.requireNonNull(planoId, "O ID do plano e obrigatorio.");

        PlanoTreino plano = planos.buscarPorId(planoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Plano nao encontrado."));

        autorizador.exigirPersonalDoPlano(sessao, plano);
        return plano;
    }

    private void validarVersaoComExerciciosAtivos(VersaoPlano versao) {
        for (Treino treino : versao.getTreinos()) {
            validarExerciciosAtivos(treino.getItens());
        }
    }

    private void validarExerciciosAtivos(List<ItemTreino> itens) {
        for (ItemTreino item : itens) {
            Objects.requireNonNull(item, "Um item de treino nao pode ser nulo.");

            Exercicio exercicio = exercicios.buscarPorId(item.exercicioId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException(
                            "Exercicio nao encontrado: " + item.exercicioId()));

            if (!exercicio.isAtivo()) {
                throw new ValorInvalidoException(
                        "Nao e permitido prescrever ou publicar exercicio inativo: "
                                + exercicio.getNome());
            }
        }
    }
}
