
package br.ufpb.jwpersonal.infra.sqlite;

import br.ufpb.jwpersonal.domain.model.Execucao;
import br.ufpb.jwpersonal.domain.model.StatusExecucao;
import br.ufpb.jwpersonal.repository.ExecucaoRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistência SQLite das execuções dos alunos.
 */
public class SqliteExecucaoRepository
        implements ExecucaoRepository {

    private final ConexaoSQLite conexao;

    public SqliteExecucaoRepository(ConexaoSQLite conexao) {
        this.conexao = Objects.requireNonNull(conexao);
    }

    @Override
    public void salvar(Execucao execucao) {

        Objects.requireNonNull(execucao);

        String sql = """
                INSERT INTO execucao (
                    id, aluno_id, atribuicao_id, plano_id,
                    numero_versao, nome_treino, data,
                    status, registrada_em
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    aluno_id = excluded.aluno_id,
                    atribuicao_id = excluded.atribuicao_id,
                    plano_id = excluded.plano_id,
                    numero_versao = excluded.numero_versao,
                    nome_treino = excluded.nome_treino,
                    data = excluded.data,
                    status = excluded.status,
                    registrada_em = excluded.registrada_em
                """;

        SqliteSupport.executar(
                conexao,
                "salvar execução",
                connection -> {

                    try (PreparedStatement ps =
                                 connection.prepareStatement(sql)) {

                        ps.setString(1, execucao.getId().toString());
                        ps.setString(2, execucao.getAlunoId().toString());
                        ps.setString(3, execucao.getAtribuicaoId().toString());
                        ps.setString(4, execucao.getPlanoId().toString());
                        ps.setInt(5, execucao.getNumeroVersao());
                        ps.setString(6, execucao.getNomeTreino());
                        ps.setString(7, execucao.getData().toString());
                        ps.setString(8, execucao.getStatus().name());
                        ps.setString(
                                9,
                                execucao.getRegistradaEm().toString()
                        );

                        ps.executeUpdate();
                    }

                    return null;
                }
        );
    }

    @Override
    public boolean existePorChaveNatural(String chaveNatural) {

        Objects.requireNonNull(chaveNatural);

        // O identificador do aluno é a primeira parte da chave.
        int separador = chaveNatural.indexOf('|');

        if (separador <= 0) {
            return false;
        }

        try {

            UUID alunoId = UUID.fromString(
                    chaveNatural.substring(0, separador)
            );

            return historicoDoAluno(alunoId)
                    .stream()
                    .anyMatch(execucao ->
                            execucao.chaveNatural()
                                    .equals(chaveNatural)
                    );

        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public boolean existeParaVersao(
            UUID planoId,
            int numeroVersao) {

        Objects.requireNonNull(planoId);

        String sql = """
                SELECT 1
                FROM execucao
                WHERE plano_id = ?
                  AND numero_versao = ?
                LIMIT 1
                """;

        return SqliteSupport.executar(
                conexao,
                "verificar execuções da versão",
                connection -> {

                    try (PreparedStatement ps =
                                 connection.prepareStatement(sql)) {

                        ps.setString(1, planoId.toString());
                        ps.setInt(2, numeroVersao);

                        try (ResultSet rs = ps.executeQuery()) {
                            return rs.next();
                        }
                    }
                }
        );
    }

    @Override
    public List<Execucao> historicoDoAluno(UUID alunoId) {

        Objects.requireNonNull(alunoId);

        String sql = """
                SELECT id, aluno_id, atribuicao_id, plano_id,
                       numero_versao, nome_treino, data,
                       status, registrada_em
                FROM execucao
                WHERE aluno_id = ?
                ORDER BY data DESC, registrada_em DESC
                """;

        return SqliteSupport.executar(
                conexao,
                "consultar histórico de execuções",
                connection -> {

                    List<Execucao> resultado = new ArrayList<>();

                    try (PreparedStatement ps =
                                 connection.prepareStatement(sql)) {

                        ps.setString(1, alunoId.toString());

                        try (ResultSet rs = ps.executeQuery()) {

                            while (rs.next()) {
                                resultado.add(mapear(rs));
                            }
                        }
                    }

                    return List.copyOf(resultado);
                }
        );
    }

    private Execucao mapear(ResultSet rs)
            throws java.sql.SQLException {

        return new Execucao(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("aluno_id")),
                UUID.fromString(rs.getString("atribuicao_id")),
                UUID.fromString(rs.getString("plano_id")),
                rs.getInt("numero_versao"),
                rs.getString("nome_treino"),
                java.time.LocalDate.parse(
                        rs.getString("data")
                ),
                StatusExecucao.valueOf(
                        rs.getString("status")
                ),
                java.time.Instant.parse(
                        rs.getString("registrada_em")
                )
        );
    }
}
