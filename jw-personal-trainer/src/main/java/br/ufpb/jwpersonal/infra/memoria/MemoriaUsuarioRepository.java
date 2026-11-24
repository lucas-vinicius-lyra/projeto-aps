
package br.ufpb.jwpersonal.infra.memoria;

import br.ufpb.jwpersonal.domain.model.Usuario;
import br.ufpb.jwpersonal.domain.excecao.ValorInvalidoException;
import br.ufpb.jwpersonal.repository.UsuarioRepository;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class MemoriaUsuarioRepository
        implements UsuarioRepository {

    private final Map<UUID, Usuario> usuarios = new HashMap<>();

    @Override
    public void salvar(Usuario usuario) {
        Objects.requireNonNull(usuario);

        boolean emailDuplicado = usuarios.values().stream()
                .anyMatch(u ->
                        u.getEmail().equals(usuario.getEmail())
                        && !u.getId().equals(usuario.getId()));

        if (emailDuplicado) {
            throw new ValorInvalidoException(
                    "Ja existe um usuario com este e-mail.");
        }

        usuarios.put(usuario.getId(), usuario);
    }

    @Override
    public Optional<Usuario> buscarPorId(UUID id) {
        return Optional.ofNullable(usuarios.get(id));
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }

        String normalizado = email.trim()
                .toLowerCase(Locale.ROOT);

        return usuarios.values().stream()
                .filter(u -> u.getEmail().equals(normalizado))
                .findFirst();
    }
}
