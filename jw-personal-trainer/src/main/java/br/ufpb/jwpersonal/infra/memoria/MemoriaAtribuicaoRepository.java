
package br.ufpb.jwpersonal.infra.memoria;

import br.ufpb.jwpersonal.domain.excecao.ValorInvalidoException;
import br.ufpb.jwpersonal.domain.model.Atribuicao;
import br.ufpb.jwpersonal.repository.AtribuicaoRepository;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class MemoriaAtribuicaoRepository
        implements AtribuicaoRepository, ParticipanteTransacaoMemoria {

    private final Map<UUID, Atribuicao> atribuicoes =
            new HashMap<>();

    @Override
    public void salvar(Atribuicao atribuicao) {
        Objects.requireNonNull(atribuicao);

        if (atribuicao.estaAtiva()) {
            boolean duplicidade = atribuicoes.values()
                    .stream()
                    .anyMatch(a ->
                            a.estaAtiva()
                                    && a.getAlunoId()
                                    .equals(atribuicao.getAlunoId())
                                    && !a.getId()
                                    .equals(atribuicao.getId()));

            if (duplicidade) {
                throw new ValorInvalidoException(
                        "O aluno ja possui uma atribuicao ativa.");
            }
        }

        atribuicoes.put(atribuicao.getId(), atribuicao);
    }

    @Override
    public Optional<Atribuicao> buscarAtivaDoAluno(
            UUID alunoId) {

        return atribuicoes.values().stream()
                .filter(a ->
                        a.getAlunoId().equals(alunoId)
                                && a.estaAtiva())
                .findFirst();
    }

    @Override
    public List<Atribuicao> historicoDoAluno(
            UUID alunoId) {

        return atribuicoes.values().stream()
                .filter(a -> a.getAlunoId().equals(alunoId))
                .sorted(Comparator.comparing(
                        Atribuicao::getInicio).reversed())
                .toList();
    }

    /**
     * Cria uma cópia independente de todas as atribuições.
     */
    @Override
    public Object criarSnapshot() {

        Map<UUID, Atribuicao> copia = new HashMap<>();

        atribuicoes.forEach((id, atribuicao) ->
                copia.put(id, copiarAtribuicao(atribuicao))
        );

        return new Snapshot(Map.copyOf(copia));
    }

    /**
     * Restaura o estado anterior do repositório.
     */
    @Override
    public void restaurarSnapshot(Object snapshot) {

        if (!(snapshot instanceof Snapshot estado)) {
            throw new IllegalArgumentException(
                    "Snapshot inválido para o repositório de atribuições."
            );
        }

        atribuicoes.clear();

        estado.atribuicoes().forEach((id, atribuicao) ->
                atribuicoes.put(id, copiarAtribuicao(atribuicao))
        );
    }

    /**
     * Copia uma atribuição preservando todos os seus dados.
     *
     * A cópia é necessária porque Atribuicao possui estado mutável:
     * status e data de encerramento.
     */
    private Atribuicao copiarAtribuicao(Atribuicao original) {

        return new Atribuicao(
                original.getId(),
                original.getAlunoId(),
                original.getPlanoId(),
                original.getNumeroVersao(),
                original.getInicio(),
                original.getFim(),
                original.getStatus()
        );
    }

    /**
     * Representa o estado salvo do repositório.
     */
    private record Snapshot(
            Map<UUID, Atribuicao> atribuicoes
    ) {
    }
}
