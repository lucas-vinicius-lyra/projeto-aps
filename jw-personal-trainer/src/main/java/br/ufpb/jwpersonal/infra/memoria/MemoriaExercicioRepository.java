
package br.ufpb.jwpersonal.infra.memoria;

import br.ufpb.jwpersonal.domain.model.Exercicio;
import br.ufpb.jwpersonal.repository.ExercicioRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementacao em memoria do ExercicioRepository.
 *
 * Utilizada durante o desenvolvimento e nos testes.
 * Os dados nao sao persistidos apos encerrar a aplicacao.
 */
public class MemoriaExercicioRepository
        implements ExercicioRepository {

    private final Map<UUID, Exercicio> exercicios =
            new HashMap<>();

    /**
     * Salva um exercicio novo ou atualiza
     * um exercicio existente.
     */
    @Override
    public void salvar(Exercicio exercicio) {

        Objects.requireNonNull(
                exercicio,
                "O exercicio nao pode ser nulo."
        );

        exercicios.put(
                exercicio.getId(),
                exercicio
        );
    }

    /**
     * Busca um exercicio pelo identificador.
     */
    @Override
    public Optional<Exercicio> buscarPorId(UUID id) {

        Objects.requireNonNull(
                id,
                "O ID do exercicio e obrigatorio."
        );

        return Optional.ofNullable(
                exercicios.get(id)
        );
    }

    /**
     * Retorna somente os exercicios ativos.
     */
    @Override
    public List<Exercicio> listarAtivos() {

        List<Exercicio> ativos = new ArrayList<>();

        for (Exercicio exercicio : exercicios.values()) {

            if (exercicio.isAtivo()) {
                ativos.add(exercicio);
            }
        }

        return List.copyOf(ativos);
    }
}
