package br.ufpb.jwpersonal.domain.model;

import br.ufpb.jwpersonal.domain.excecao.ValorInvalidoException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Registro de conclusão de um treino por um aluno.
 * Não registra cargas ou repetições realizadas: o escopo revisado registra apenas a conclusão.
 */
public final class Execucao {

    private final UUID id;
    private final UUID alunoId;
    private final UUID atribuicaoId;
    private final UUID planoId;
    private final int numeroVersao;
    private final String nomeTreino;
    private final LocalDate data;
    private final StatusExecucao status;
    private final Instant registradaEm;

    public Execucao(UUID alunoId, UUID atribuicaoId,
                    UUID planoId, int numeroVersao,
                    String nomeTreino, LocalDate data) {

        this(
                UUID.randomUUID(),
                alunoId,
                atribuicaoId,
                planoId,
                numeroVersao,
                nomeTreino,
                data,
                StatusExecucao.FINALIZADO,
                Instant.now()
        );
    }

    public Execucao(UUID id, UUID alunoId, UUID atribuicaoId,
                    UUID planoId, int numeroVersao,
                    String nomeTreino, LocalDate data,
                    StatusExecucao status, Instant registradaEm) {

        this.id = Objects.requireNonNull(
                id, "O ID da execução é obrigatório.");

        this.alunoId = Objects.requireNonNull(
                alunoId, "O aluno é obrigatório.");

        this.atribuicaoId = Objects.requireNonNull(
                atribuicaoId, "A atribuição é obrigatória.");

        this.planoId = Objects.requireNonNull(
                planoId, "O plano é obrigatório.");

        if (numeroVersao < 1) {
            throw new ValorInvalidoException(
                    "O número da versão deve ser positivo.");
        }

        this.numeroVersao = numeroVersao;

        if (nomeTreino == null || nomeTreino.isBlank()) {
            throw new ValorInvalidoException(
                    "O nome do treino é obrigatório.");
        }

        this.nomeTreino = nomeTreino.trim().replaceAll("\\s+", " ");

        this.data = Objects.requireNonNull(
                data, "A data da execução é obrigatória.");

        this.status = Objects.requireNonNull(
                status, "O status da execução é obrigatório.");

        if (status != StatusExecucao.FINALIZADO) {
            throw new ValorInvalidoException(
                    "Uma execução registrada deve estar FINALIZADA.");
        }

        this.registradaEm = Objects.requireNonNull(
                registradaEm, "A data e hora do registro são obrigatórias.");
    }

    /**
     * Chave usada pelo repositório para impedir duplicidade da RN06:
     * aluno + plano + versão + treino + data.
     */
    public String chaveNatural() {

        String nomeNormalizado = nomeTreino.toLowerCase(Locale.ROOT);

        return alunoId + "|"
                + planoId + "|"
                + numeroVersao + "|"
                + nomeNormalizado + "|"
                + data;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAlunoId() {
        return alunoId;
    }

    public UUID getAtribuicaoId() {
        return atribuicaoId;
    }

    public UUID getPlanoId() {
        return planoId;
    }

    public int getNumeroVersao() {
        return numeroVersao;
    }

    public String getNomeTreino() {
        return nomeTreino;
    }

    public LocalDate getData() {
        return data;
    }

    public StatusExecucao getStatus() {
        return status;
    }

    public Instant getRegistradaEm() {
        return registradaEm;
    }
}