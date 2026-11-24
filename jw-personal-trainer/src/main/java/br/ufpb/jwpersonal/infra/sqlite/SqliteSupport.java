
package br.ufpb.jwpersonal.infra.sqlite;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Métodos auxiliares para execução de operações JDBC.
 */
final class SqliteSupport {

    private SqliteSupport() {
    }

    @FunctionalInterface
    interface OperacaoSQL<T> {
        T executar(Connection conexao) throws SQLException;
    }

    /**
     * Obtém uma conexão, executa a operação e libera a conexão
     * quando ela não pertence a uma transação ativa.
     */
    static <T> T executar(
            ConexaoSQLite gerenciador,
            String descricao,
            OperacaoSQL<T> operacao) {

        Connection conexao = null;

        try {

            conexao = gerenciador.obterConexao();

            return operacao.executar(conexao);

        } catch (SQLException e) {

            throw new PersistenciaSQLiteException(
                    "Falha ao executar operação SQLite: " + descricao,
                    e
            );

        } finally {

            if (conexao != null) {
                gerenciador.liberarConexao(conexao);
            }
        }
    }
}
