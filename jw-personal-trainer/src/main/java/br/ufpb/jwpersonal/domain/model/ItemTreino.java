package br.ufpb.jwpersonal.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Prescrição imutável de um exercício dentro de um treino.
 * Record usado para garantir que alterações produzam um novo ItemTreino.
 */
public record ItemTreino(
        UUID exercicioId,
        Series series,
        Repeticoes repeticoes,
        Carga carga
) {

    public ItemTreino {

        Objects.requireNonNull(exercicioId, "O exercício é obrigatório.");

        Objects.requireNonNull(
                series, "A quantidade de séries é obrigatória.");

        Objects.requireNonNull(
                repeticoes, "A quantidade de repetições é obrigatória.");

        Objects.requireNonNull(
                carga, "A carga é obrigatória.");
    }

    public ItemTreino comSeries(Series novasSeries) {
        return new ItemTreino(
                exercicioId, novasSeries, repeticoes, carga);
    }

    public ItemTreino comRepeticoes(Repeticoes novasRepeticoes) {
        return new ItemTreino(
                exercicioId, series, novasRepeticoes, carga);
    }

    public ItemTreino comCarga(Carga novaCarga) {
        return new ItemTreino(
                exercicioId, series, repeticoes, novaCarga);
    }
}