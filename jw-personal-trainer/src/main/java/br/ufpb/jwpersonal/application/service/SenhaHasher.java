package br.ufpb.jwpersonal.application.service;

public interface SenhaHasher {
    String gerarHash(String senha);

    boolean verificar(String senha, String hashArmazenado);
}
