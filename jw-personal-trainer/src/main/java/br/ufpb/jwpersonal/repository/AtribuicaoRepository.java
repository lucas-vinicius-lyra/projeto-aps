package br.ufpb.jwpersonal.repository;

import br.ufpb.jwpersonal.domain.model.Atribuicao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Contrato de persistência das atribuições de planos.
 */
public interface AtribuicaoRepository {

    void salvar(Atribuicao atribuicao);

    Optional<Atribuicao> buscarAtivaDoAluno(UUID alunoId);

    List<Atribuicao> historicoDoAluno(UUID alunoId);
}