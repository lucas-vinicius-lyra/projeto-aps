
package br.ufpb.jwpersonal.infra.memoria;

import br.ufpb.jwpersonal.domain.model.Atribuicao;
import br.ufpb.jwpersonal.domain.model.StatusAtribuicao;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MemoriaUnidadeDeTrabalhoTest {

    @Test
    void deveRestaurarAtribuicaoQuandoOperacaoFalhar() {

        MemoriaAtribuicaoRepository repository =
                new MemoriaAtribuicaoRepository();

        UUID alunoId = UUID.randomUUID();

        UUID planoAntigo = UUID.randomUUID();
        UUID planoNovo = UUID.randomUUID();

        Atribuicao anterior = new Atribuicao(
                alunoId,
                planoAntigo,
                1,
                LocalDate.now().minusDays(5)
        );

        repository.salvar(anterior);

        MemoriaUnidadeDeTrabalho unidade =
                new MemoriaUnidadeDeTrabalho(repository);

        // Simula uma operação que falha depois de modificar
        // a atribuição anterior e salvar a nova.
        assertThrows(
                IllegalStateException.class,
                () -> unidade.executar(() -> {

                    Atribuicao atual = repository
                            .buscarAtivaDoAluno(alunoId)
                            .orElseThrow();

                    atual.encerrar(LocalDate.now());

                    repository.salvar(atual);

                    Atribuicao nova = new Atribuicao(
                            alunoId,
                            planoNovo,
                            1,
                            LocalDate.now()
                    );

                    repository.salvar(nova);

                    // Simula uma falha durante a operação.
                    throw new IllegalStateException(
                            "Falha simulada durante a atribuição."
                    );
                })
        );

        // Depois do rollback, a atribuição anterior
        // deve continuar ativa.
        Atribuicao restaurada = repository
                .buscarAtivaDoAluno(alunoId)
                .orElseThrow();

        assertEquals(
                anterior.getId(),
                restaurada.getId()
        );

        assertTrue(restaurada.estaAtiva());

        assertEquals(
                1,
                repository.historicoDoAluno(alunoId).size()
        );
    }

    @Test
    void deveManterAlteracoesQuandoOperacaoConcluir() {

        MemoriaAtribuicaoRepository repository =
                new MemoriaAtribuicaoRepository();

        UUID alunoId = UUID.randomUUID();

        Atribuicao anterior = new Atribuicao(
                alunoId,
                UUID.randomUUID(),
                1,
                LocalDate.now().minusDays(5)
        );

        repository.salvar(anterior);

        MemoriaUnidadeDeTrabalho unidade =
                new MemoriaUnidadeDeTrabalho(repository);

        unidade.executar(() -> {

            Atribuicao atual = repository
                    .buscarAtivaDoAluno(alunoId)
                    .orElseThrow();

            atual.encerrar(LocalDate.now());

            repository.salvar(atual);

            Atribuicao nova = new Atribuicao(
                    alunoId,
                    UUID.randomUUID(),
                    1,
                    LocalDate.now()
            );

            repository.salvar(nova);

            return null;
        });

        List<Atribuicao> historico =
                repository.historicoDoAluno(alunoId);

        assertEquals(2, historico.size());

        assertTrue(
                historico.stream()
                        .anyMatch(Atribuicao::estaAtiva)
        );

        assertTrue(
                historico.stream()
                        .anyMatch(a ->
                                a.getStatus()
                                        == StatusAtribuicao.ENCERRADA)
        );
    }
}
