
package br.ufpb.jwpersonal.infra.sqlite;

import br.ufpb.jwpersonal.domain.model.Atribuicao;
import br.ufpb.jwpersonal.domain.model.StatusAtribuicao;
import br.ufpb.jwpersonal.repository.AtribuicaoRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistência SQLite das atribuições de planos.
 */
public class SqliteAtribuicaoRepository
        implements AtribuicaoRepository {

    private final ConexaoSQLite conexao;

    public SqliteAtribuicaoRepository(ConexaoSQLite conexao) {
        this.conexao = Objects.requireNonNull(conexao);
    }

    @Override
    public void salvar(Atribuicao atribuicao) {

        Objects.requireNonNull(atribuicao);

        String sql = """
                INSERT INTO atribuicao (
                    id, aluno_id, plano_id, numero_versao,
                    inicio, fim, status
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    aluno_id = excluded.aluno_id,
                    plano_id = excluded.plano_id,
                    numero_versao = excluded.numero_versao,
                    inicio = excluded.inicio,
                    fim = excluded.fim,
                    status = excluded.status
                """;

        SqliteSupport.executar(
                conexao,
                "salvar atribuição",
                connection -> {

                    try (PreparedStatement ps =
                                 connection.prepareStatement(sql)) {

                        ps.setString(1, atribuicao.getId().toString());
                        ps.setString(2, atribuicao.getAlunoId().toString());
                        ps.setString(3, atribuicao.getPlanoId().toString());
                        ps.setInt(4, atribuicao.getNumeroVersao());
                        ps.setString(5, atribuicao.getInicio().toString());

                        if (atribuicao.getFim() == null) {
                            ps.setNull(6, java.sql.Types.VARCHAR);
                        } else {
                            ps.setString(6, atribuicao.getFim().toString());
                        }

                        ps.setString(7, atribuicao.getStatus().name());

                        ps.executeUpdate();
                    }

                    return null;
                }
        );
    }

    @Override
    public Optional<Atribuicao> buscarAtivaDoAluno(UUID alunoId) {

        Objects.requireNonNull(alunoId);

        String sql = """
                SELECT id, aluno_id, plano_id, numero_versao,
                       inicio, fim, status
                FROM atribuicao
                WHERE aluno_id = ?
                  AND status = 'ATIVA'
                """;

        return SqliteSupport.executar(
                conexao,
                "buscar atribuição ativa",
                connection -> {

                    try (PreparedStatement ps =
                                 connection.prepareStatement(sql)) {

                        ps.setString(1, alunoId.toString());

                        try (ResultSet rs = ps.executeQuery()) {

                            if (!rs.next()) {
                                return Optional.empty();
                            }

                            return Optional.of(mapear(rs));
                        }
                    }
                }
        );
    }

    @Override
    public List<Atribuicao> historicoDoAluno(UUID alunoId) {

        Objects.requireNonNull(alunoId);

        String sql = """
                SELECT id, aluno_id, plano_id, numero_versao,
                       inicio, fim, status
                FROM atribuicao
                WHERE aluno_id = ?
                ORDER BY inicio DESC, rowid DESC
                """;

        return SqliteSupport.executar(
                conexao,
                "consultar histórico de atribuições",
                connection -> {

                    List<Atribuicao> resultado = new ArrayList<>();

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

    private Atribuicao mapear(ResultSet rs)
            throws java.sql.SQLException {

        String fim = rs.getString("fim");

        return new Atribuicao(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("aluno_id")),
                UUID.fromString(rs.getString("plano_id")),
                rs.getInt("numero_versao"),
                java.time.LocalDate.parse(
                        rs.getString("inicio")
                ),
                fim == null
                        ? null
                        : java.time.LocalDate.parse(fim),
                StatusAtribuicao.valueOf(
                        rs.getString("status")
                )
        );
    }
}
