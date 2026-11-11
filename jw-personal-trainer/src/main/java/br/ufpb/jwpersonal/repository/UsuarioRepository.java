package br.ufpb.jwpersonal.repository;

import br.ufpb.jwpersonal.domain.model.Usuario;

import java.util.Optional;
import java.util.UUID;

/**
 * Contrato de persistência dos usuários.
 */
public interface UsuarioRepository {

    void salvar(Usuario usuario);

    Optional<Usuario> buscarPorId(UUID id);

    Optional<Usuario> buscarPorEmail(String email);
}