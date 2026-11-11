package br.ufpb.jwpersonal.repository;

import br.ufpb.jwpersonal.domain.model.Execucao;

import java.util.List;
import java.util.UUID;

/**
 * Contrato de persistência das execuções de treino.
 */
public interface ExecucaoRepository {

    void salvar(Execucao execucao);

    boolean existePorChaveNatural(String chaveNatural);

    boolean existeParaVersao(UUID planoId, int numeroVersao);

    List<Execucao> historicoDoAluno(UUID alunoId);
}