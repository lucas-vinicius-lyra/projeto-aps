package br.ufpb.jwpersonal.domain.model;

import br.ufpb.jwpersonal.domain.excecao.TransicaoInvalidaException;
import br.ufpb.jwpersonal.domain.excecao.ValorInvalidoException;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Vínculo entre um aluno e uma versão específica de um plano publicado.
 */
public final class Atribuicao {

    private final UUID id;
    private final UUID alunoId;
    private final UUID planoId;
    private final int numeroVersao;
    private final LocalDate inicio;

    private LocalDate fim;
    private StatusAtribuicao status;

    public Atribuicao(UUID alunoId, UUID planoId,
                      int numeroVersao, LocalDate inicio) {

        this(
                UUID.randomUUID(),
                alunoId,
                planoId,
                numeroVersao,
                inicio,
                null,
                StatusAtribuicao.ATIVA
        );
    }

    public Atribuicao(UUID id, UUID alunoId, UUID planoId,
                      int numeroVersao, LocalDate inicio,
                      LocalDate fim, StatusAtribuicao status) {

        this.id = Objects.requireNonNull(
                id, "O ID da atribuição é obrigatório.");

        this.alunoId = Objects.requireNonNull(
                alunoId, "O aluno é obrigatório.");

        this.planoId = Objects.requireNonNull(
                planoId, "O plano é obrigatório.");

        if (numeroVersao < 1) {
            throw new ValorInvalidoException(
                    "O número da versão deve ser positivo.");
        }

        this.numeroVersao = numeroVersao;

        this.inicio = Objects.requireNonNull(
                inicio, "A data de início é obrigatória.");

        this.status = Objects.requireNonNull(
                status, "O status da atribuição é obrigatório.");

        if (status == StatusAtribuicao.ATIVA && fim != null) {
            throw new ValorInvalidoException(
                    "Uma atribuição ativa não pode possuir data de fim.");
        }

        if (status == StatusAtribuicao.ENCERRADA && fim == null) {
            throw new ValorInvalidoException(
                    "Uma atribuição encerrada deve possuir data de fim.");
        }

        if (fim != null && fim.isBefore(inicio)) {
            throw new ValorInvalidoException(
                    "A data de fim não pode ser anterior à data de início.");
        }

        this.fim = fim;
    }

    /**
     * Encerra a atribuição, preservando sua data de início e histórico.
     */
    public void encerrar(LocalDate data) {

        Objects.requireNonNull(
                data, "A data de encerramento é obrigatória.");

        if (!estaAtiva()) {
            throw new TransicaoInvalidaException(
                    "A atribuição já está encerrada.");
        }

        if (data.isBefore(inicio)) {
            throw new ValorInvalidoException(
                    "A data de encerramento não pode ser anterior ao início da atribuição.");
        }

        fim = data;
        status = StatusAtribuicao.ENCERRADA;
    }

    /**
     * Verifica se a atribuição está ativa.
     */
    public boolean estaAtiva() {
        return status == StatusAtribuicao.ATIVA;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAlunoId() {
        return alunoId;
    }

    public UUID getPlanoId() {
        return planoId;
    }

    public int getNumeroVersao() {
        return numeroVersao;
    }

    public LocalDate getInicio() {
        return inicio;
    }

    public LocalDate getFim() {
        return fim;
    }

    public StatusAtribuicao getStatus() {
        return status;
    }
}