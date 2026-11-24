
package br.ufpb.jwpersonal.infra.memoria;

import br.ufpb.jwpersonal.domain.model.PlanoTreino;
import br.ufpb.jwpersonal.repository.PlanoRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class MemoriaPlanoRepository
        implements PlanoRepository {

    private final Map<UUID, PlanoTreino> planos = new HashMap<>();

    @Override
    public void salvar(PlanoTreino plano) {
        Objects.requireNonNull(
                plano, "O plano nao pode ser nulo.");

        planos.put(plano.getId(), plano);
    }

    @Override
    public Optional<PlanoTreino> buscarPorId(UUID id) {
        return Optional.ofNullable(planos.get(id));
    }

    @Override
    public List<PlanoTreino> listarPorPersonal(UUID personalId) {
        Objects.requireNonNull(personalId);

        return planos.values().stream()
                .filter(p ->
                        p.getPersonalId().equals(personalId))
                .toList();
    }
}
