package br.ufpb.jwpersonal.domain.fabrica;

import br.ufpb.jwpersonal.domain.excecao.ValorInvalidoException;
import br.ufpb.jwpersonal.domain.model.ObjetivoPlano;
import br.ufpb.jwpersonal.domain.model.Treino;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Representa um modelo inicial de plano de treino.
 *
 * Contém o objetivo e os nomes das sessões que serão criadas
 * para facilitar a montagem inicial de um plano pelo personal.
 */
public record ModeloInicialPlano(
        ObjetivoPlano objetivo,
        List<String> nomesTreinos
) {

    public ModeloInicialPlano {

        Objects.requireNonNull(
                objetivo,
                "O objetivo do plano é obrigatório."
        );

        Objects.requireNonNull(
                nomesTreinos,
                "A lista de treinos é obrigatória."
        );

        if (nomesTreinos.isEmpty() || nomesTreinos.size() > 7) {
            throw new ValorInvalidoException(
                    "O modelo deve possuir entre 1 e 7 treinos."
            );
        }

        List<String> nomesValidados = new ArrayList<>();
        Set<String> nomesNormalizados = new HashSet<>();

        for (String nome : nomesTreinos) {

            if (nome == null || nome.isBlank()) {
                throw new ValorInvalidoException(
                        "O nome dos treinos não pode ser vazio."
                );
            }

            String nomeValidado = nome.trim()
                    .replaceAll("\\s+", " ");

            String nomeNormalizado = nomeValidado
                    .toLowerCase(Locale.ROOT);

            if (!nomesNormalizados.add(nomeNormalizado)) {
                throw new ValorInvalidoException(
                        "O modelo não pode possuir nomes de treinos repetidos."
                );
            }

            nomesValidados.add(nomeValidado);
        }

        nomesTreinos = List.copyOf(nomesValidados);
    }

    /**
     * Cria novas sessões de treino vazias a partir do modelo.
     *
     * Cada chamada produz novos objetos Treino,
     * permitindo que o personal os complete posteriormente.
     */
    public List<Treino> criarTreinos() {

        return nomesTreinos.stream()
                .map(Treino::new)
                .toList();
    }
}