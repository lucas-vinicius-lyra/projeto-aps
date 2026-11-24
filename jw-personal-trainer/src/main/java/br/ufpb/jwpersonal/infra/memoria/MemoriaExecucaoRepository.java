package br.ufpb.jwpersonal.infra.memoria;

import br.ufpb.jwpersonal.domain.excecao.ValorInvalidoException;
import br.ufpb.jwpersonal.domain.model.Execucao;
import br.ufpb.jwpersonal.repository.ExecucaoRepository;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class MemoriaExecucaoRepository
        implements ExecucaoRepository {

    private final Map<UUID, Execucao> execucoes = new HashMap<>();

    @Override
    public void salvar(Execucao execucao) {
        Objects.requireNonNull(execucao);

        boolean duplicada = execucoes.values().stream()
                .anyMatch(e ->
                        e.chaveNatural()
                                .equals(execucao.chaveNatural())
                        && !e.getId().equals(execucao.getId()));

        if (duplicada) {
            throw new ValorInvalidoException(
                    "Esta execucao ja foi registrada.");
        }

        execucoes.put(execucao.getId(), execucao);
    }

    @Override
    public boolean existePorChaveNatural(
            String chaveNatural) {

        Objects.requireNonNull(chaveNatural);

        return execucoes.values().stream()
                .anyMatch(e ->
                        e.chaveNatural().equals(chaveNatural));
    }

    @Override
    public boolean existeParaVersao(
            UUID planoId, int numeroVersao) {

        return execucoes.values().stream()
                .anyMatch(e ->
                        e.getPlanoId().equals(planoId)
                        && e.getNumeroVersao() == numeroVersao);
    }

    @Override
    public List<Execucao> historicoDoAluno(
            UUID alunoId) {

        return execucoes.values().stream()
                .filter(e ->
                        e.getAlunoId().equals(alunoId))
                .sorted(Comparator.comparing(
                        Execucao::getData).reversed())
                .toList();
    }
}
 
