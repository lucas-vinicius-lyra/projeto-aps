package br.ufpb.jwpersonal.application.service;

import java.util.UUID;

public record AlunoResumo(UUID id, String nome, String email) {
}