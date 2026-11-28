package br.ufpb.jwpersonal.ui;

import br.ufpb.jwpersonal.application.facade.JWPersonalTrainerFacade;
import br.ufpb.jwpersonal.application.service.ResultadoRegistroExecucao;
import br.ufpb.jwpersonal.application.service.SessaoUsuario;
import br.ufpb.jwpersonal.domain.model.*;

import java.io.PrintStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

final class MenuAluno {

    private final JWPersonalTrainerFacade facade;
    private final SessaoUsuario sessao;
    private final EntradaConsole entrada;
    private final PrintStream saida;
    private final Apresentador apresentador;

    MenuAluno(JWPersonalTrainerFacade facade, SessaoUsuario sessao,
              EntradaConsole entrada) {
        this.facade = facade;
        this.sessao = sessao;
        this.entrada = entrada;
        this.saida = entrada.saida();
        this.apresentador = new Apresentador(facade, sessao, saida);
    }

    void exibir() {
        while (true) {
            saida.println("""

                    === MENU DO ALUNO ===
                    1. Consultar plano ativo
                    2. Ver treinos (séries, repetições e carga)
                    3. Ver instruções, referências e vídeos
                    4. Registrar conclusão de treino
                    5. Histórico de execuções
                    6. Notificações de novas atribuições
                    7. Histórico de atribuições
                    0. Sair da conta""");

            int opcao = entrada.lerOpcao(7);

            if (opcao == 0) {
                return;
            }

            Acoes.executar(saida, () -> {
                switch (opcao) {
                    case 1 -> planoAtivo();
                    case 2 -> verTreinos();
                    case 3 -> verApoio();
                    case 4 -> registrar();
                    case 5 -> historicoExecucoes();
                    case 6 -> notificacoes();
                    case 7 -> historicoAtribuicoes();
                    default -> throw new IllegalStateException("Opção sem ação: " + opcao);
                }
            });
        }
    }

    private Optional<VersaoPlano> versaoAtiva() {
        Optional<VersaoPlano> versao = facade.consultarMeuPlanoAtivo(sessao);

        if (versao.isEmpty()) {
            saida.println("Você não possui plano ativo.");
        }

        return versao;
    }

    private void planoAtivo() {
        facade.buscarMinhaAtribuicaoAtiva(sessao).ifPresentOrElse(a -> {
            saida.println("Plano: " + facade.consultarTituloDoPlano(sessao, a.getPlanoId())
                    + " (versão " + a.getNumeroVersao() + ")");
            saida.println("Ativo desde " + a.getInicio().format(EntradaConsole.DATA));

            versaoAtiva().ifPresent(v -> v.getTreinos().forEach(t ->
                    saida.println("- " + t.getNome() + ": "
                            + t.getItens().size() + " exercício(s)")));
        }, () -> saida.println("Você não possui plano ativo."));
    }

    private void verTreinos() {
        versaoAtiva().ifPresent(v -> apresentador.treinos(v.getTreinos()));
    }

    private void verApoio() {
        versaoAtiva().ifPresent(v -> v.getTreinos().stream()
                .flatMap(t -> t.getItens().stream())
                .map(ItemTreino::exercicioId)
                .distinct()
                .forEach(id -> apresentador.apoio(facade.consultarExercicio(sessao, id))));
    }

    private void registrar() {
        Optional<VersaoPlano> versao = versaoAtiva();

        if (versao.isEmpty()) {
            return;
        }

        Optional<Treino> treino = entrada.escolher(
                "Qual treino você concluiu?", versao.get().getTreinos(), Treino::getNome);

        if (treino.isEmpty()) {
            return;
        }

        LocalDate data = entrada.lerData("Data da execução");

        ResultadoRegistroExecucao resultado = facade.registrarExecucao(
                sessao, treino.get().getNome(), data);

        String descricao = resultado.execucao().getNomeTreino() + " em "
                + resultado.execucao().getData().format(EntradaConsole.DATA);

        saida.println(resultado.jaExistia()
                ? "Já registrado: " + descricao + ". Nenhuma duplicidade foi criada."
                : "Sucesso: execução registrada - " + descricao + ".");
    }

    private void historicoExecucoes() {
        List<Execucao> execucoes = facade.consultarMeuHistoricoExecucoes(sessao);

        if (execucoes.isEmpty()) {
            saida.println("Nenhuma execução registrada.");
        }

        execucoes.forEach(e -> saida.println("- "
                + e.getData().format(EntradaConsole.DATA) + " | " + e.getNomeTreino()
                + " | " + facade.consultarTituloDoPlano(sessao, e.getPlanoId())
                + " v" + e.getNumeroVersao()));
    }

    private void notificacoes() {
        var notificacoes = facade.consultarMinhasNotificacoes(sessao);

        if (notificacoes.isEmpty()) {
            saida.println("Nenhuma notificação desde que a aplicação foi aberta.");
        }

        notificacoes.forEach(n -> saida.println("- "
                + n.dataAtribuicao().format(EntradaConsole.DATA) + ": " + n.mensagem()));
    }

    private void historicoAtribuicoes() {
        List<Atribuicao> historico = facade.consultarMeuHistoricoAtribuicoes(sessao);

        if (historico.isEmpty()) {
            saida.println("Nenhuma atribuição.");
        }

        historico.forEach(a -> saida.println("- "
                + facade.consultarTituloDoPlano(sessao, a.getPlanoId())
                + " v" + a.getNumeroVersao() + " | " + a.getStatus()
                + " | início " + a.getInicio().format(EntradaConsole.DATA)));
    }
}