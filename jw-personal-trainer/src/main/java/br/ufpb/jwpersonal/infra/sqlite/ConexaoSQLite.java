
package br.ufpb.jwpersonal.infra.sqlite;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

/**
 * Gerencia conexões com o banco SQLite.
 *
 * Compartilha a conexão da transação atual entre os repositórios
 * da mesma thread.
 */
public class ConexaoSQLite {

    private final String jdbcUrl;

    private final ThreadLocal<Connection> transacaoAtual =
            new ThreadLocal<>();

    /**
     * Recebe o caminho do arquivo do banco.
     *
     * Exemplo: data/jw-personal-trainer.db
     */
    public ConexaoSQLite(Path arquivoBanco) {

        Objects.requireNonNull(
                arquivoBanco,
                "O caminho do banco é obrigatório."
        );

        try {

            Path absoluto = arquivoBanco
                    .toAbsolutePath()
                    .normalize();

            Path diretorio = absoluto.getParent();

            if (diretorio != null) {
                Files.createDirectories(diretorio);
            }

            this.jdbcUrl = "jdbc:sqlite:" + absoluto;

        } catch (Exception e) {

            throw new PersistenciaSQLiteException(
                    "Não foi possível preparar o arquivo do banco.",
                    e
            );
        }
    }

    /**
     * Retorna uma conexão para os repositórios.
     *
     * Durante uma transação, retorna a conexão compartilhada.
     * Fora de uma transação, abre uma nova conexão.
     */
    public Connection obterConexao() throws SQLException {

        Connection atual = transacaoAtual.get();

        if (atual != null && !atual.isClosed()) {
            return atual;
        }

        return abrirNovaConexao();
    }

    /**
     * Abre uma nova conexão e configura o SQLite.
     */
    private Connection abrirNovaConexao() throws SQLException {

        Connection conexao = DriverManager.getConnection(jdbcUrl);

        try (Statement statement = conexao.createStatement()) {

            // Ativa as chaves estrangeiras nesta conexão.
            statement.execute("PRAGMA foreign_keys = ON");

            // Define o tempo máximo de espera por um bloqueio.
            statement.execute("PRAGMA busy_timeout = 5000");
        }

        return conexao;
    }

    /**
     * Fecha somente conexões independentes.
     *
     * A conexão de uma transação não pode ser fechada
     * pelos repositórios.
     */
    public void liberarConexao(Connection conexao) {

        if (conexao == null) {
            return;
        }

        if (conexao == transacaoAtual.get()) {
            return;
        }

        try {
            conexao.close();

        } catch (SQLException e) {

            throw new PersistenciaSQLiteException(
                    "Não foi possível fechar a conexão SQLite.",
                    e
            );
        }
    }

    /**
     * Inicia uma nova conexão transacional.
     */
    Connection iniciarTransacao() throws SQLException {

        if (transacaoAtual.get() != null) {
            throw new IllegalStateException(
                    "Já existe uma transação ativa nesta thread."
            );
        }

        Connection conexao = abrirNovaConexao();

        try {

            conexao.setAutoCommit(false);

            transacaoAtual.set(conexao);

            return conexao;

        } catch (SQLException e) {

            conexao.close();

            throw e;
        }
    }

    /**
     * Finaliza a transação e libera a conexão compartilhada.
     */
    void finalizarTransacao(Connection conexao) {

        if (conexao != transacaoAtual.get()) {
            throw new IllegalStateException(
                    "A conexão informada não pertence à transação atual."
            );
        }

        transacaoAtual.remove();

        try {
            conexao.close();

        } catch (SQLException e) {

            throw new PersistenciaSQLiteException(
                    "Não foi possível fechar a conexão transacional.",
                    e
            );
        }
    }

    /**
     * Informa se existe uma transação ativa nesta thread.
     */
    boolean possuiTransacaoAtiva() {
        return transacaoAtual.get() != null;
    }
}
