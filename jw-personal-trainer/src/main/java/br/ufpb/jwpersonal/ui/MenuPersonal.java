package br.ufpb.jwpersonal.ui;

import br.ufpb.jwpersonal.application.facade.JWPersonalTrainerFacade;
import br.ufpb.jwpersonal.application.service.AlunoResumo;
import br.ufpb.jwpersonal.application.service.SessaoUsuario;
import br.ufpb.jwpersonal.domain.model.*;
import br.ufpb.jwpersonal.domain.politica.*;

import java.io.PrintStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

final class MenuPersonal {

    private final JWPersonalTrainerFacade facade;
    private final SessaoUsuario sessao;
    private final EntradaConsole entrada;
    private final PrintStream saida;
    private final Apresentador apresentador;
    private final EditorTreino editor;

    MenuPersonal(JWPersonalTrainerFacade facade, SessaoUsuario sessao,
                 EntradaConsole entrada) {
        this.facade = facade;
        this.sessao = sessao;
        this.entrada = entrada;
        this.saida = entrada.saida();
        this.apresentador = new Apresentador(facade, sessao, saida);
        this.editor = new EditorTreino(facade, sessao, entrada, apresentador);
    }

    void exibir() {
        while (true) {
            saida.println("""

                    === MENU DO PERSONAL ===
                     1. Consultar meus planos
                     2. Consultar versões de um plano
                     3. Cadastrar exercício
                     4. Editar instruções, vídeo e referência de exercício
                     5. Inativar exercício
                     6. Prescrever plano
                     7. Iniciar revisão de plano
                     8. Editar rascunho (treinos e itens)
                     9. Aplicar progressão de carga
                    10. Aplicar progressão de volume
                    11. Aplicar dupla progressão
                    12. Publicar versão
                    13. Corrigir versão publicada
                    14. Atribuir plano a aluno
                    15. Consultar execuções de um aluno
                    16. Consultar atribuições de um aluno
                    17. Consultar catálogo de exercícios
                     0. Sair da conta""");

            int opcao = entrada.lerOpcao(17);

            if (opcao == 0) {
                return;
            }

            Acoes.executar(saida, () -> {
                switch (opcao) {
                    case 1 -> listarPlanos();
                    case 2 -> consultarVersoes();
                    case 3 -> cadastrarExercicio();
                    case 4 -> editarExercicio();
                    case 5 -> inativarExercicio();
                    case 6 -> prescrever();
                    case 7 -> iniciarRevisao();
                    case 8 -> editarRascunho();
                    case 9 -> progressaoCarga();
                    case 10 -> progressaoVolume();
                    case 11 -> progressaoDupla();
                    case 12 -> publicar();
                    case 13 -> corrigir();
                    case 14 -> atribuir();
                    case 15 -> execucoesDoAluno();
                    case 16 -> atribuicoesDoAluno();
                    case 17 -> listarCatalogo();
                    default -> throw new IllegalStateException("Opção sem ação: " + opcao);
                }
            });
        }
    }

    // ---------------------------------------------------------------- planos

    private String resumo(PlanoTreino plano) {
        return plano.getTitulo() + " [" + plano.getObjetivo() + "] - publicada: "
                + plano.versaoPublicada().map(v -> "v" + v.getNumero()).orElse("nenhuma")
                + " | rascunho: "
                + plano.rascunhoAberto().map(v -> "v" + v.getNumero()).orElse("nenhum");
    }

    private Optional<PlanoTreino> escolherPlano(String titulo, Predicate<PlanoTreino> filtro) {
        List<PlanoTreino> planos = facade.listarMeusPlanos(sessao).stream()
                .filter(filtro)
                .toList();

        return entrada.escolher(titulo, planos, this::resumo);
    }

    private Optional<PlanoTreino> escolherComRascunho() {
        return escolherPlano("Planos com rascunho aberto",
                p -> p.rascunhoAberto().isPresent());
    }

    private Optional<PlanoTreino> escolherPublicado() {
        return escolherPlano("Planos publicados",
                p -> p.versaoPublicada().isPresent());
    }

    /** Relê o plano para refletir o que foi de fato persistido. */
    private PlanoTreino recarregar(UUID planoId) {
        return facade.listarMeusPlanos(sessao).stream()
                .filter(p -> p.getId().equals(planoId))
                .findFirst()
                .orElseThrow();
    }

    private void listarPlanos() {
        List<PlanoTreino> planos = facade.listarMeusPlanos(sessao);

        if (planos.isEmpty()) {
            saida.println("Você ainda não possui planos.");
        }

        planos.forEach(p -> saida.println("- " + resumo(p)));
    }

    private void consultarVersoes() {
        escolherPlano("Escolha o plano", p -> true).ifPresent(plano ->
                facade.consultarHistoricoVersoes(sessao, plano.getId())
                        .forEach(apresentador::versao));
    }

    private void prescrever() {
        String titulo = entrada.lerObrigatorio("Título do plano");

        Optional<ObjetivoPlano> objetivo = entrada.escolher(
                "Objetivo", List.of(ObjetivoPlano.values()), Enum::name);

        if (objetivo.isEmpty()) {
            return;
        }

        List<Treino> iniciais = entrada.confirmar(
                "Começar pelo modelo inicial do objetivo?")
                ? facade.gerarModeloInicial(sessao, objetivo.get()).criarTreinos()
                : List.of();

        Optional<List<Treino>> treinos = editor.editarTreinos(iniciais);

        if (treinos.isEmpty()) {
            saida.println("Prescrição cancelada.");
            return;
        }

        PlanoTreino plano = facade.prescreverPlano(
                sessao, titulo, objetivo.get(), treinos.get());

        saida.println("Sucesso: plano salvo com a versão 1 em RASCUNHO.");

        if (entrada.confirmar("Publicar agora?")) {
            facade.publicarPlano(sessao, plano.getId(), 1);
            saida.println("Sucesso: versão 1 publicada.");
        }
    }

    private void iniciarRevisao() {
        Optional<PlanoTreino> plano = escolherPublicado();

        if (plano.isEmpty()) {
            return;
        }

        VersaoPlano nova = facade.iniciarRevisao(sessao, plano.get().getId());

        saida.println("Sucesso: versão " + nova.getNumero() + " criada em RASCUNHO.");
        saida.println("Use as opções 8 a 12 para ajustar e publicar.");
        apresentador.versao(nova);
    }

    private void editarRascunho() {
        Optional<PlanoTreino> escolhido = escolherComRascunho();

        if (escolhido.isEmpty()) {
            return;
        }

        UUID planoId = escolhido.get().getId();

        while (true) {
            VersaoPlano rascunho = recarregar(planoId).rascunhoAberto().orElseThrow();
            int numero = rascunho.getNumero();

            saida.println();
            apresentador.versao(rascunho);
            saida.println("1. Adicionar treino");
            saida.println("2. Remover treino");
            saida.println("3. Ajustar itens de um treino");
            saida.println("0. Voltar");

            int opcao = entrada.lerOpcao(3);

            if (opcao == 0) {
                return;
            }

            Acoes.executar(saida, () -> {
                if (opcao == 1) {
                    editor.novoTreino().ifPresent(treino -> {
                        facade.adicionarTreino(sessao, planoId, numero, treino);
                        saida.println("Sucesso: treino adicionado.");
                    });
                } else if (opcao == 2) {
                    editor.escolherTreino(rascunho.getTreinos()).ifPresent(treino -> {
                        facade.removerTreino(sessao, planoId, numero, treino.getNome());
                        saida.println("Sucesso: treino removido.");
                    });
                } else {
                    editor.escolherTreino(rascunho.getTreinos()).ifPresent(atual ->
                            editor.editarItens(atual).ifPresent(novo -> {
                                facade.substituirTreino(sessao, planoId, numero,
                                        atual.getNome(), novo);
                                saida.println("Sucesso: treino atualizado.");
                            }));
                }
            });
        }
    }

    // ---------------------------------------------------- progressão (RN08)

    private void progressaoCarga() {
        escolherComRascunho().ifPresent(plano -> aplicar(plano,
                new ProgressaoCarga(entrada.lerDecimal(
                        "Percentual de aumento", new java.math.BigDecimal("5")))));
    }

    private void progressaoVolume() {
        escolherComRascunho().ifPresent(plano ->
                aplicar(plano, new ProgressaoVolume()));
    }

    private void progressaoDupla() {
        escolherComRascunho().ifPresent(plano -> aplicar(plano,
                new ProgressaoDupla(
                        entrada.lerInteiro("Repetições mínimas da faixa",
                                ProgressaoDupla.REPETICOES_MINIMAS_PADRAO),
                        entrada.lerInteiro("Repetições máximas da faixa",
                                ProgressaoDupla.REPETICOES_MAXIMAS_PADRAO),
                        entrada.lerInteiro("Repetições acrescentadas por vez",
                                ProgressaoDupla.INCREMENTO_REPETICOES_PADRAO),
                        entrada.lerDecimal("Percentual de aumento de carga",
                                ProgressaoDupla.PERCENTUAL_CARGA_PADRAO))));
    }

    private void aplicar(PlanoTreino plano, PoliticaProgressao politica) {
        int numero = plano.rascunhoAberto().orElseThrow().getNumero();

        facade.aplicarProgressao(sessao, plano.getId(), numero, politica);

        saida.println("Sucesso: progressão aplicada ao rascunho.");
        apresentador.versao(recarregar(plano.getId()).versaoPorNumero(numero));
    }

    private void publicar() {
        Optional<PlanoTreino> plano = escolherComRascunho();

        if (plano.isEmpty()) {
            return;
        }

        VersaoPlano rascunho = plano.get().rascunhoAberto().orElseThrow();
        apresentador.versao(rascunho);

        if (entrada.confirmar("Publicar esta versão?")) {
            facade.publicarPlano(sessao, plano.get().getId(), rascunho.getNumero());
            saida.println("Sucesso: versão " + rascunho.getNumero()
                    + " publicada. A versão publicada anterior, se houver, foi arquivada.");
            saida.println("Alunos já atribuídos continuam na versão anterior"
                    + " até uma nova atribuição (opção 14).");
        }
    }

    private void corrigir() {
        Optional<PlanoTreino> plano = escolherPublicado();

        if (plano.isEmpty()) {
            return;
        }

        UUID planoId = plano.get().getId();
        VersaoPlano publicada = plano.get().versaoPublicada().orElseThrow();

        if (!facade.podeCorrigirDiretamente(sessao, planoId, publicada.getNumero())) {
            saida.println("Esta versão já possui execução registrada e não pode ser"
                    + " alterada. Inicie uma revisão (opção 7).");
            return;
        }

        List<Treino> copias = publicada.getTreinos().stream()
                .map(Treino::copiar)
                .toList();

        Optional<List<Treino>> corrigidos = editor.editarTreinos(copias);

        if (corrigidos.isEmpty()) {
            saida.println("Correção cancelada.");
            return;
        }

        facade.corrigirVersao(sessao, planoId, publicada.getNumero(), corrigidos.get());
        saida.println("Sucesso: versão " + publicada.getNumero() + " corrigida.");
    }

    // ------------------------------------------------------- CU03 e alunos

    private AlunoResumo localizarAluno() {
        AlunoResumo aluno = facade.localizarAlunoPorEmail(
                sessao, entrada.lerObrigatorio("E-mail do aluno"));

        saida.println("Aluno: " + aluno.nome() + " <" + aluno.email() + ">");

        return aluno;
    }

    private void atribuir() {
        AlunoResumo aluno = localizarAluno();

        Optional<PlanoTreino> plano = escolherPublicado();

        if (plano.isEmpty()) {
            return;
        }

        VersaoPlano publicada = plano.get().versaoPublicada().orElseThrow();
        apresentador.versao(publicada);

        if (!entrada.confirmar("Atribuir esta versão ao aluno?")) {
            return;
        }

        List<UUID> anteriores = facade.consultarAtribuicoesDoAluno(sessao, aluno.id())
                .stream()
                .map(Atribuicao::getId)
                .toList();

        Atribuicao atribuicao = facade.atribuirPlano(
                sessao, aluno.id(), plano.get().getId(), publicada.getNumero());

        if (anteriores.contains(atribuicao.getId())) {
            saida.println("Já registrado: o aluno já está com esta versão ativa."
                    + " Nada foi duplicado.");
        } else {
            saida.println("Sucesso: plano atribuído. A atribuição ativa anterior,"
                    + " se houver, foi encerrada.");
        }
    }

    private void execucoesDoAluno() {
        AlunoResumo aluno = localizarAluno();
        List<Execucao> execucoes = facade.consultarHistoricoExecucoes(sessao, aluno.id());

        if (execucoes.isEmpty()) {
            saida.println("Nenhuma execução registrada nos seus planos.");
        }

        execucoes.forEach(e -> saida.println("- "
                + e.getData().format(EntradaConsole.DATA) + " | " + e.getNomeTreino()
                + " | " + facade.consultarTituloDoPlano(sessao, e.getPlanoId())
                + " v" + e.getNumeroVersao()));
    }

    private void atribuicoesDoAluno() {
        AlunoResumo aluno = localizarAluno();
        List<Atribuicao> historico = facade.consultarAtribuicoesDoAluno(sessao, aluno.id());

        if (historico.isEmpty()) {
            saida.println("Nenhuma atribuição com os seus planos.");
        }

        historico.forEach(a -> saida.println("- "
                + facade.consultarTituloDoPlano(sessao, a.getPlanoId())
                + " v" + a.getNumeroVersao() + " | " + a.getStatus()
                + " | início " + a.getInicio().format(EntradaConsole.DATA)
                + (a.getFim() == null ? "" : " | fim " + a.getFim().format(EntradaConsole.DATA))));
    }

    // -------------------------------------------------------------- catálogo

    private void listarCatalogo() {
        List<Exercicio> ativos = facade.listarExerciciosAtivos(sessao);

        if (ativos.isEmpty()) {
            saida.println("Catálogo vazio.");
        }

        ativos.forEach(apresentador::apoio);
    }

    private Optional<Exercicio> escolherExercicio() {
        return entrada.escolher("Exercícios ativos",
                facade.listarExerciciosAtivos(sessao), Exercicio::getNome);
    }

    private void cadastrarExercicio() {
        Exercicio novo = facade.cadastrarExercicio(
                sessao,
                entrada.lerObrigatorio("Nome"),
                entrada.lerTexto("Instruções (opcional)"),
                entrada.lerTexto("Link do vídeo http/https (opcional)"),
                entrada.lerTexto("Fonte de referência (opcional)"));

        saida.println("Sucesso: exercício \"" + novo.getNome() + "\" cadastrado.");
    }

    private void editarExercicio() {
        Optional<Exercicio> escolhido = escolherExercicio();

        if (escolhido.isEmpty()) {
            return;
        }

        Exercicio atual = escolhido.get();
        apresentador.apoio(atual);
        saida.println("Enter mantém o valor atual; \"-\" remove vídeo ou referência.");

        String instrucoes = manter(entrada.lerTexto("Novas instruções"), atual.getInstrucoes());
        String video = manter(entrada.lerTexto("Novo link do vídeo"), atual.getVideoUrl());
        String fonte = manter(entrada.lerTexto("Nova referência"), atual.getFonteReferencia());

        facade.atualizarExercicio(sessao, atual.getId(), instrucoes,
                "-".equals(video) ? null : video,
                "-".equals(fonte) ? null : fonte);

        saida.println("Sucesso: conteúdo atualizado. As prescrições não foram alteradas.");
    }

    private static String manter(String digitado, String atual) {
        return digitado.isEmpty() ? atual : digitado;
    }

    private void inativarExercicio() {
        escolherExercicio().ifPresent(exercicio -> {
            if (entrada.confirmar("Inativar \"" + exercicio.getNome() + "\"?")) {
                facade.inativarExercicio(sessao, exercicio.getId());
                saida.println("Sucesso: exercício inativado. O histórico foi preservado.");
            }
        });
    }
}