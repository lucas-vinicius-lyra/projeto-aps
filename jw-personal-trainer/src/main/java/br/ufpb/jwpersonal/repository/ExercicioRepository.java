package br.ufpb.jwpersonal.repository;

import br.ufpb.jwpersonal.domain.model.Exercicio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Contrato de persistência dos exercícios.
 */
public interface ExercicioRepository {

    void salvar(Exercicio exercicio);

    Optional<Exercicio> buscarPorId(UUID id);

    List<Exercicio> listarAtivos();
}