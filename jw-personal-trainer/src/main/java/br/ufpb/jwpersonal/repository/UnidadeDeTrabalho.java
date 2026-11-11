package br.ufpb.jwpersonal.repository;

import java.util.function.Supplier;

/**
 * Contrato que coordena operações atômicas de persistência.
 * Todas as gravações da operação devem ser confirmadas juntas
 * ou desfeitas em caso de falha.
 */
public interface UnidadeDeTrabalho {

    /**
     * Executa uma operação dentro de uma unidade de trabalho.
     *
     * A implementação concreta será responsável pelo commit
     * ou rollback conforme o resultado da operação.
     */
    <T> T executar(Supplier<T> operacao);
}