
package br.ufpb.jwpersonal.infra.sqlite;

import br.ufpb.jwpersonal.repository.UnidadeDeTrabalho;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Implementação de UnidadeDeTrabalho utilizando transações JDBC.
 */
public class SqliteUnidadeDeTrabalho
        implements UnidadeDeTrabalho {

    private final ConexaoSQLite conexao;

    public SqliteUnidadeDeTrabalho(ConexaoSQLite conexao) {
        this.conexao = Objects.requireNonNull(conexao);
    }

    @Override
    public <T> T executar(Supplier<T> operacao) {

        Objects.requireNonNull(
                operacao,
                "A operação é obrigatória."
        );

        Connection connection;

        try {
            // Abre a conexão e inicia a transação.
            connection = conexao.iniciarTransacao();

        } catch (SQLException e) {

            throw new PersistenciaSQLiteException(
                    "Não foi possível iniciar a transação.",
                    e
            );
        }

        try {

            // Executa todas as operações dos repositórios
            // usando a mesma conexão transacional.
            T resultado = operacao.get();

            // Confirma todas as alterações.
            connection.commit();

            return resultado;

        } catch (RuntimeException | Error falha) {

            realizarRollback(connection, falha);

            throw falha;

        } catch (SQLException falha) {

            PersistenciaSQLiteException erro =
                    new PersistenciaSQLiteException(
                            "Não foi possível confirmar a transação.",
                            falha
                    );

            realizarRollback(connection, erro);

            throw erro;

        } finally {

            conexao.finalizarTransacao(connection);
        }
    }

    /**
     * Desfaz todas as alterações da transação.
     */
    private void realizarRollback(
            Connection connection,
            Throwable falhaOriginal) {

        try {

            connection.rollback();

        } catch (SQLException erroRollback) {

            // Preserva a falha original e registra
            // também o problema ocorrido durante o rollback.
            falhaOriginal.addSuppressed(erroRollback);
        }
    }
}
