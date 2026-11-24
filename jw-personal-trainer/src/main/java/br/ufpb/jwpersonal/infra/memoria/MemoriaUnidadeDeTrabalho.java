package br.ufpb.jwpersonal.infra.memoria;

import br.ufpb.jwpersonal.repository.UnidadeDeTrabalho;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Implementacao inicial da unidade de trabalho em memoria.
 *
 * ATENCAO: ainda nao implementa rollback.
 * Nao deve ser utilizada para comprovar a RN05.
 */
public class MemoriaUnidadeDeTrabalho
        implements UnidadeDeTrabalho {

    @Override
    public synchronized <T> T executar(
            Supplier<T> operacao) {

        Objects.requireNonNull(
                operacao, "A operacao e obrigatoria.");

        return operacao.get();
    }
}