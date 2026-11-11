package br.ufpb.jwpersonal.domain.model;

import br.ufpb.jwpersonal.domain.excecao.ValorInvalidoException;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Representa uma pessoa cadastrada no sistema, com um perfil de acesso.
 * A senha deve ser recebida já protegida por hash; o domínio não armazena senha pura.
 */
public final class Usuario {

    private final UUID id;
    private final String nome;
    private final String email;
    private final String senhaHash;
    private final Perfil perfil;
    private boolean ativo;

    public Usuario(String nome, String email, String senhaHash, Perfil perfil) {
        this(UUID.randomUUID(), nome, email, senhaHash, perfil, true);
    }

    public Usuario(UUID id, String nome, String email, String senhaHash,
                   Perfil perfil, boolean ativo) {

        this.id = Objects.requireNonNull(id, "O ID do usuário é obrigatório.");

        this.nome = validarTextoObrigatorio(
                nome, "O nome do usuário é obrigatório.");

        this.email = validarEmail(email);

        this.senhaHash = validarTextoObrigatorio(
                senhaHash, "O hash da senha é obrigatório.");

        this.perfil = Objects.requireNonNull(
                perfil, "O perfil do usuário é obrigatório.");

        this.ativo = ativo;
    }

    private static String validarTextoObrigatorio(String valor, String mensagem) {

        if (valor == null || valor.isBlank()) {
            throw new ValorInvalidoException(mensagem);
        }

        return valor.trim();
    }

    private static String validarEmail(String email) {

        String normalizado = validarTextoObrigatorio(
                email, "O e-mail do usuário é obrigatório.")
                .toLowerCase(Locale.ROOT);

        if (!normalizado.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new ValorInvalidoException("O e-mail informado é inválido.");
        }

        return normalizado;
    }

    /**
     * Verifica se o usuário possui determinado perfil.
     */
    public boolean possuiPerfil(Perfil perfilVerificado) {
        return perfilVerificado != null && perfil == perfilVerificado;
    }

    /**
     * Inativa a conta do usuário.
     */
    public void inativar() {
        ativo = false;
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public boolean isAtivo() {
        return ativo;
    }
}