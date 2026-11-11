package br.ufpb.jwpersonal.domain.model;

import br.ufpb.jwpersonal.domain.excecao.RecursoNaoEncontradoException;
import br.ufpb.jwpersonal.domain.excecao.TransicaoInvalidaException;
import br.ufpb.jwpersonal.domain.excecao.ValorInvalidoException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Agrupa os exercícios de uma sessão, por exemplo, "Treino A".
 * Um rascunho pode ser montado gradualmente; antes da publicação ele deve
 * possuir de 1 a 12 itens. Depois de publicado, seus itens ficam bloqueados.
 */
public final class Treino {

    private final String nome;
    private final List<ItemTreino> itens;
    private boolean bloqueado;

    public Treino(String nome) {
        this(nome, List.of());
    }

    public Treino(String nome, List<ItemTreino> itens) {

        if (nome == null || nome.isBlank()) {
            throw new ValorInvalidoException(
                    "O nome do treino é obrigatório.");
        }

        this.nome = nome.trim();

        Objects.requireNonNull(itens, "A lista de itens é obrigatória.");

        if (itens.size() > 12) {
            throw new ValorInvalidoException(
                    "Um treino pode possuir no máximo 12 exercícios.");
        }

        this.itens = new ArrayList<>(itens);

        if (this.itens.stream().anyMatch(Objects::isNull)) {
            throw new ValorInvalidoException(
                    "O treino não pode conter itens nulos.");
        }
    }

    /**
     * Adiciona um exercício ao treino.
     */
    public void adicionarItem(ItemTreino item) {

        verificarSeEditavel();

        Objects.requireNonNull(item, "O item do treino é obrigatório.");

        if (itens.size() >= 12) {
            throw new ValorInvalidoException(
                    "Um treino pode possuir no máximo 12 exercícios.");
        }

        itens.add(item);
    }

    /**
     * Remove um exercício do treino.
     */
    public void removerItem(ItemTreino item) {

        verificarSeEditavel();

        if (item == null || !itens.remove(item)) {
            throw new RecursoNaoEncontradoException(
                    "O exercício informado não está neste treino.");
        }
    }

    /**
     * Aplica uma estratégia a todos os itens, substituindo-os por novos records.
     */
    public void aplicarProgressao(Function<ItemTreino, ItemTreino> estrategia) {

        verificarSeEditavel();

        substituirItens(calcularItensProgressionados(estrategia));
    }

    /**
     * Calcula os novos itens sem alterar o treino,
     * permitindo aplicação atômica na versão.
     */
    List<ItemTreino> calcularItensProgressionados(
            Function<ItemTreino, ItemTreino> estrategia) {

        Objects.requireNonNull(
                estrategia, "A estratégia de progressão é obrigatória.");

        return itens.stream()
                .map(estrategia)
                .map(item -> Objects.requireNonNull(
                        item, "A estratégia não pode retornar um item nulo."))
                .toList();
    }

    /**
     * Retorna uma cópia editável, usada para criar uma nova versão em rascunho.
     */
    public Treino copiar() {
        return new Treino(nome, itens);
    }

    /**
     * Bloqueia a edição do treino.
     * Utilizado quando uma versão é publicada.
     */
    void bloquearEdicao() {
        bloqueado = true;
    }

    /**
     * Substitui os itens do treino.
     * Utilizado internamente pelo versionamento e pela progressão.
     */
    void substituirItens(List<ItemTreino> novosItens) {

        verificarSeEditavel();

        Objects.requireNonNull(
                novosItens, "A lista de itens é obrigatória.");

        if (novosItens.size() > 12
                || novosItens.stream().anyMatch(Objects::isNull)) {

            throw new ValorInvalidoException(
                    "A lista deve conter de 0 a 12 itens válidos.");
        }

        itens.clear();
        itens.addAll(novosItens);
    }

    /**
     * Verifica a quantidade de itens antes da publicação.
     */
    void validarParaPublicacao() {

        if (itens.isEmpty() || itens.size() > 12) {
            throw new ValorInvalidoException(
                    "Cada treino deve possuir entre 1 e 12 exercícios para ser publicado.");
        }
    }

    private void verificarSeEditavel() {

        if (bloqueado) {
            throw new TransicaoInvalidaException(
                    "Não é permitido alterar um treino de uma versão publicada ou arquivada.");
        }
    }

    public String getNome() {
        return nome;
    }

    public List<ItemTreino> getItens() {
        return List.copyOf(itens);
    }

    public boolean isBloqueado() {
        return bloqueado;
    }
}