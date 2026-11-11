package br.ufpb.jwpersonal.repository;

import br.ufpb.jwpersonal.domain.model.PlanoTreino;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Contrato de persistência dos planos e suas versões.
 */
public interface PlanoRepository {

    void salvar(PlanoTreino plano);

    Optional<PlanoTreino> buscarPorId(UUID id);

    List<PlanoTreino> listarPorPersonal(UUID personalId);
}