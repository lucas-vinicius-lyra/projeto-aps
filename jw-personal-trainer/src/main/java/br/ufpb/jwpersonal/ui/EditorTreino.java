package br.ufpb.jwpersonal.ui;

import br.ufpb.jwpersonal.application.facade.JWPersonalTrainerFacade;
import br.ufpb.jwpersonal.application.service.SessaoUsuario;
import br.ufpb.jwpersonal.domain.excecao.RegraDeNegocioException;
import br.ufpb.jwpersonal.domain.model.*;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

final class EditorTreino {

    private final JWPersonalTrainerFacade facade;
    private final SessaoUsuario sessao;
    private final EntradaConsole entrada;
    private final PrintStream saida;
    private final Apresentador apresentador;

    EditorTreino(JWPersonalTrainerFacade facade, SessaoUsuario sessao,
                 EntradaConsole entrada, Apresentador apresentador) {
        this.facade = facade;
        this.sessao = sessao;
        this.entrada = entrada;
        this.saida = entrada.saida();
        this.apresentador = apresentador;
    }

    Optional<List<Treino>> editarTreinos(List<Treino> iniciais) {
        VersaoPlano rascunhoLocal = new VersaoPlano(1);
        iniciais.forEach(rascunhoLocal::adicionarTreino);

        while (true) {
            saida.println();
            saida.println("--- Treinos em edição ---");
            apresentador.treinos(rascunhoLocal.getTreinos());
            saida.println("1. Adicionar treino");
            saida.println("2. Remover treino");
            saida.println("3. Ajustar itens de um treino");
            saida.println("4. Concluir e salvar");
            saida.println("0. Cancelar sem salvar");

            int opcao = entrada.lerOpcao(4);

            if (opcao == 0) {
                return Optional.empty();
            }

            if (opcao == 4) {
                return Optional.of(rascunhoLocal.getTreinos());
            }

            Acoes.executar(saida, () -> {
                if (opcao == 1) {
                    novoTreino().ifPresent(rascunhoLocal::adicionarTreino);
                } else if (opcao == 2) {
                    escolherTreino(rascunhoLocal.getTreinos())
                            .ifPresent(t -> rascunhoLocal.removerTreino(t.getNome()));
                } else {
                    escolherTreino(rascunhoLocal.getTreinos()).ifPresent(t ->
                            editarItens(t).ifPresent(novo ->
                                    rascunhoLocal.substituirTreino(t.getNome(), novo)));
                }
            });
        }
    }

    Optional<Treino> novoTreino() {
        String nome = entrada.lerObrigatorio("Nome do treino (ex.: Treino A)");

        return editarItens(new Treino(nome));
    }

    Optional<Treino> escolherTreino(List<Treino> treinos) {
        return entrada.escolher("Escolha o treino", treinos, Treino::getNome);
    }

    Optional<Treino> editarItens(Treino original) {
        List<ItemTreino> itens = new ArrayList<>(original.getItens());

        while (true) {
            saida.println();
            saida.println("--- " + original.getNome() + " ---");
            apresentador.itens(itens);
            saida.println("1. Adicionar exercício");
            saida.println("2. Ajustar exercício, séries, repetições ou carga");
            saida.println("3. Remover exercício");
            saida.println("4. Concluir treino");
            saida.println("0. Cancelar");

            int opcao = entrada.lerOpcao(4);

            if (opcao == 0) {
                return Optional.empty();
            }

            if (opcao == 4) {
                return Optional.of(new Treino(original.getNome(), itens));
            }

            Acoes.executar(saida, () -> {
                if (opcao == 1) {
                    lerItem(null).ifPresent(item -> {
                        // O domínio rejeita o 13º item.
                        new Treino(original.getNome(), itens).adicionarItem(item);
                        itens.add(item);
                    });
                    return;
                }

                escolherPosicao(itens).ifPresent(posicao -> {
                    if (opcao == 2) {
                        lerItem(itens.get(posicao))
                                .ifPresent(item -> itens.set(posicao, item));
                    } else {
                        itens.remove((int) posicao);
                    }
                });
            });
        }
    }

    private Optional<Integer> escolherPosicao(List<ItemTreino> itens) {
        List<Integer> posicoes = new ArrayList<>();

        for (int i = 0; i < itens.size(); i++) {
            posicoes.add(i);
        }

        return entrada.escolher("Escolha o item", posicoes,
                i -> apresentador.descrever(itens.get(i)));
    }

    private Optional<ItemTreino> lerItem(ItemTreino atual) {
        java.util.UUID exercicioId;

        if (atual != null && !entrada.confirmar("Trocar o exercício?")) {
            exercicioId = atual.exercicioId();
        } else {
            Optional<Exercicio> escolhido = entrada.escolher(
                    "Exercícios do catálogo",
                    facade.listarExerciciosAtivos(sessao),
                    Exercicio::getNome);

            if (escolhido.isEmpty()) {
                return Optional.empty();
            }

            exercicioId = escolhido.get().getId();
        }

        Series series = ateSerValido(() -> new Series(entrada.lerInteiro(
                "Séries (1 a 10)",
                atual == null ? null : atual.series().valor())));

        Repeticoes repeticoes = ateSerValido(() -> new Repeticoes(entrada.lerInteiro(
                "Repetições (1 a 50)",
                atual == null ? null : atual.repeticoes().valor())));

        Carga carga = ateSerValido(() -> new Carga(entrada.lerDecimal(
                "Carga em kg (0 a 500)",
                atual == null ? null : atual.carga().valor())));

        return Optional.of(new ItemTreino(exercicioId, series, repeticoes, carga));
    }

    private <T> T ateSerValido(Supplier<T> leitura) {
        while (true) {
            try {
                return leitura.get();
            } catch (RegraDeNegocioException e) {
                saida.println("Erro: " + e.getMessage());
            }
        }
    }
}