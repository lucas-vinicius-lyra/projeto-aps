package br.ufpb.jwpersonal.application.service;

import br.ufpb.jwpersonal.domain.excecao.ValorInvalidoException;
import br.ufpb.jwpersonal.domain.model.Perfil;
import br.ufpb.jwpersonal.domain.model.Usuario;

import java.util.Objects;
import java.util.UUID;

public record SessaoUsuario(UUID usuarioId, String nome, String email, Perfil perfil) {

    public SessaoUsuario {
        Objects.requireNonNull(usuarioId, "O ID do usuário é obrigatório.");
        Objects.requireNonNull(perfil, "O perfil do usuário é obrigatório.");

        if (nome == null || nome.isBlank()) {
            throw new ValorInvalidoException("O nome do usuário é obrigatório.");
        }

        if (email == null || email.isBlank()) {
            throw new ValorInvalidoException("O e-mail do usuário é obrigatório.");
        }
    }

    public static SessaoUsuario de(Usuario usuario) {
        Objects.requireNonNull(usuario, "O usuário é obrigatório.");

        return new SessaoUsuario(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPerfil()
        );
    }

    public boolean ehPersonal() {
        return perfil == Perfil.PERSONAL;
    }

    public boolean ehAluno() {
        return perfil == Perfil.ALUNO;
    }

    public boolean pertenceA(UUID id) {
        return usuarioId.equals(id);
    }
}
