
package br.ufpb.jwpersonal.application.service;

import br.ufpb.jwpersonal.domain.excecao.RecursoNaoEncontradoException;
import br.ufpb.jwpersonal.domain.excecao.ValorInvalidoException;
import br.ufpb.jwpersonal.domain.fabrica.GeradorDePlano;
import br.ufpb.jwpersonal.domain.fabrica.ModeloInicialPlano;
import br.ufpb.jwpersonal.domain.model.*;
import br.ufpb.jwpersonal.domain.politica.PoliticaProgressao;
import br.ufpb.jwpersonal.repository.*;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Coordena os casos de uso de prescrição e revisão (CU01 e CU02).
 *
 * Também utiliza o padrão Factory Method para gerar modelos
 * iniciais de acordo com o objetivo do plano.
 */
public class PrescricaoService {

    private final PlanoRepository planos;
    private final ExercicioRepository exercicios;
    private final ExecucaoRepository execucoes;
    private final UnidadeDeTrabalho unidadeDeTrabalho;
    private final Autorizador autorizador;

    private final Map<ObjetivoPlano, GeradorDePlano> geradoresPorObjetivo;

    public PrescricaoService(
            PlanoRepository planos,
            ExercicioRepository exercicios,
            ExecucaoRepository execucoes,
            UnidadeDeTrabalho unidadeDeTrabalho,
            Autorizador autorizador,
            Map<ObjetivoPlano, GeradorDePlano> geradoresPorObjetivo) {

        this.planos = Objects.requireNonNull(planos);
        this.exercicios = Objects.requireNonNull(exercicios);
        this.execucoes = Objects.requireNonNull(execucoes);
        this.unidadeDeTrabalho = Objects.requireNonNull(unidadeDeTrabalho);
        this.autorizador = Objects.requireNonNull(autorizador);

        Objects.requireNonNull(
                geradoresPorObjetivo,
                "Os geradores de plano são obrigatórios."
        );

        // Copia os geradores para evitar alterações externas no registro.
        Map<ObjetivoPlano, GeradorDePlano> copia =
                new EnumMap<>(ObjetivoPlano.class);

        copia.putAll(geradoresPorObjetivo);

        // Garante que exista um gerador para cada objetivo cadastrado.
        for (ObjetivoPlano objetivo : ObjetivoPlano.values()) {

            if (!copia.containsKey(objetivo)) {
                throw new IllegalArgumentException(
                        "Não existe gerador configurado para o objetivo: "
                                + objetivo
                );
            }
        }

        this.geradoresPorObjetivo = Map.copyOf(copia);
    }

    /**
     * Factory Method:
     * gera um modelo inicial de acordo com o objetivo escolhido.
     *
     * Somente usuários PERSONAL podem solicitar modelos.
     */
    public ModeloInicialPlano gerarModeloInicial(
            SessaoUsuario sessao,
            ObjetivoPlano objetivo) {

        autorizador.exigirPersonal(sessao);

        Objects.requireNonNull(
                objetivo,
                "O objetivo do plano é obrigatório."
        );

        GeradorDePlano gerador =
                geradoresPorObjetivo.get(objetivo);

        if (gerador == null) {
            throw new ValorInvalidoException(
                    "Não existe um modelo disponível para esse objetivo."
            );
        }

        ModeloInicialPlano modelo = gerador.gerarModeloInicial();

        // Garante que o gerador selecionado produziu o objetivo solicitado.
        if (modelo.objetivo() != objetivo) {
            throw new IllegalStateException(
                    "O gerador retornou um modelo com objetivo incorreto."
            );
        }

        return modelo;
    }

    /**
     * CU01: cria o plano com a versão 1 em RASCUNHO.
     */
    public PlanoTreino prescrever(
            SessaoUsuario sessao,
            String titulo,
            ObjetivoPlano objetivo,
            List<Treino> treinos) {

        autorizador.exigirPersonal(sessao);

        Objects.requireNonNull(
                treinos,
                "A lista de treinos é obrigatória."
        );

        PlanoTreino plano = new PlanoTreino(
                titulo,
                objetivo,
                sessao.usuarioId()
        );

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

    /**
     * CU02: copia a versão publicada para o próximo rascunho.
     */
    public VersaoPlano iniciarRevisao(
            SessaoUsuario sessao,
            UUID planoId) {

        PlanoTreino plano = buscarPlanoAutorizado(sessao, planoId);

        return unidadeDeTrabalho.executar(() -> {
            VersaoPlano revisao = plano.iniciarRevisao();
            planos.salvar(plano);
            return revisao;
        });
    }

    /**
     * Adiciona uma sessão a um rascunho.
     */
    public void adicionarTreino(
            SessaoUsuario sessao,
            UUID planoId,
            int numeroVersao,
            Treino treino) {

        PlanoTreino plano = buscarPlanoAutorizado(sessao, planoId);

        Objects.requireNonNull(
                treino,
                "O treino é obrigatório."
        );

        validarExerciciosAtivos(treino.getItens());

        unidadeDeTrabalho.executar(() -> {
            plano.versaoPorNumero(numeroVersao).adicionarTreino(treino);
            planos.salvar(plano);
            return null;
        });
    }

    /**
     * Remove uma sessão do rascunho para permitir substituí-la.
     */
    public void removerTreino(
            SessaoUsuario sessao,
            UUID planoId,
            int numeroVersao,
            String nomeTreino) {

        PlanoTreino plano = buscarPlanoAutorizado(sessao, planoId);

        unidadeDeTrabalho.executar(() -> {
            plano.versaoPorNumero(numeroVersao).removerTreino(nomeTreino);
            planos.salvar(plano);
            return null;
        });
    }

    /**
     * Ajusta um treino do rascunho (itens ou nome) em uma única transação.
     */
    public void substituirTreino(
            SessaoUsuario sessao,
            UUID planoId,
            int numeroVersao,
            String nomeAtual,
            Treino novoTreino) {

        PlanoTreino plano = buscarPlanoAutorizado(sessao, planoId);

        Objects.requireNonNull(
                novoTreino,
                "O treino é obrigatório."
        );

        validarExerciciosAtivos(novoTreino.getItens());

        unidadeDeTrabalho.executar(() -> {
            plano.versaoPorNumero(numeroVersao)
                    .substituirTreino(nomeAtual, novoTreino);
            planos.salvar(plano);
            return null;
        });
    }

    /**
     * RN04: informa se a versão publicada ainda aceita correção direta.
     * Serve para avisar o usuário antes da edição; a regra continua sendo
     * aplicada em {@link #corrigirVersao}.
     */
    public boolean podeCorrigirDiretamente(
            SessaoUsuario sessao,
            UUID planoId,
            int numeroVersao) {

        PlanoTreino plano = buscarPlanoAutorizado(sessao, planoId);

        return plano.versaoPorNumero(numeroVersao).getEstado()
                == EstadoVersao.PUBLICADA
                && !execucoes.existeParaVersao(planoId, numeroVersao);
    }

    /**
     * CU02 / RN08: aplica uma política Strategy ao rascunho.
     */
    public void aplicarProgressao(
            SessaoUsuario sessao,
            UUID planoId,
            int numeroVersao,
            PoliticaProgressao politica) {

        PlanoTreino plano = buscarPlanoAutorizado(sessao, planoId);

        Objects.requireNonNull(
                politica,
                "A política é obrigatória."
        );

        unidadeDeTrabalho.executar(() -> {
            VersaoPlano versao = plano.versaoPorNumero(numeroVersao);
            versao.aplicarProgressao(politica);
            planos.salvar(plano);
            return null;
        });
    }

    /**
     * RN02/RN09: publica o rascunho e arquiva a versão anterior.
     */
    public void publicar(
            SessaoUsuario sessao,
            UUID planoId,
            int numeroVersao) {

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

    /**
     * RN04: só permite corrigir versão publicada sem execuções.
     */
    public void corrigirVersao(
            SessaoUsuario sessao,
            UUID planoId,
            int numeroVersao,
            List<Treino> treinosCorrigidos) {

        PlanoTreino plano = buscarPlanoAutorizado(sessao, planoId);

        Objects.requireNonNull(
                treinosCorrigidos,
                "Os treinos são obrigatórios."
        );

        for (Treino treino : treinosCorrigidos) {
            Objects.requireNonNull(
                    treino,
                    "Um treino não pode ser nulo."
            );

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
            SessaoUsuario sessao,
            UUID planoId) {

        autorizador.exigirPersonal(sessao);

        Objects.requireNonNull(
                planoId,
                "O ID do plano é obrigatório."
        );

        PlanoTreino plano = planos.buscarPorId(planoId)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "Plano não encontrado."
                        )
                );

        autorizador.exigirPersonalDoPlano(sessao, plano);

        return plano;
    }

    private void validarVersaoComExerciciosAtivos(
            VersaoPlano versao) {

        for (Treino treino : versao.getTreinos()) {
            validarExerciciosAtivos(treino.getItens());
        }
    }

    private void validarExerciciosAtivos(
            List<ItemTreino> itens) {

        for (ItemTreino item : itens) {

            Objects.requireNonNull(
                    item,
                    "Um item de treino não pode ser nulo."
            );

            Exercicio exercicio = exercicios.buscarPorId(item.exercicioId())
                    .orElseThrow(() ->
                            new RecursoNaoEncontradoException(
                                    "Exercício não encontrado: "
                                            + item.exercicioId()
                            )
                    );

            if (!exercicio.isAtivo()) {
                throw new ValorInvalidoException(
                        "Não é permitido prescrever ou publicar exercício inativo: "
                                + exercicio.getNome()
                );
            }
        }
    }
}
