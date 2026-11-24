
package br.ufpb.jwpersonal.infra.sqlite;

import br.ufpb.jwpersonal.domain.model.Exercicio;
import br.ufpb.jwpersonal.repository.ExercicioRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistência SQLite do catálogo de exercícios.
 */
public class SqliteExercicioRepository
        implements ExercicioRepository {

    private final ConexaoSQLite conexao;

    public SqliteExercicioRepository(ConexaoSQLite conexao) {
        this.conexao = Objects.requireNonNull(conexao);
    }

    @Override
    public void salvar(Exercicio exercicio) {

        Objects.requireNonNull(exercicio);

        String sql = """
                INSERT INTO exercicio (
                    id, nome, instrucoes, video_url,
                    fonte_referencia, ativo
                )
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    nome = excluded.nome,
                    instrucoes = excluded.instrucoes,
                    video_url = excluded.video_url,
                    fonte_referencia = excluded.fonte_referencia,
                    ativo = excluded.ativo
                """;

        SqliteSupport.executar(
                conexao,
                "salvar exercício",
                connection -> {

                    try (PreparedStatement ps =
                                 connection.prepareStatement(sql)) {

                        ps.setString(1, exercicio.getId().toString());
                        ps.setString(2, exercicio.getNome());
                        ps.setString(3, exercicio.getInstrucoes());
                        ps.setString(4, exercicio.getVideoUrl());
                        ps.setString(5, exercicio.getFonteReferencia());
                        ps.setInt(6, exercicio.isAtivo() ? 1 : 0);

                        ps.executeUpdate();
                    }

                    return null;
                }
        );
    }

    @Override
    public Optional<Exercicio> buscarPorId(UUID id) {

        Objects.requireNonNull(id);

        String sql = """
                SELECT id, nome, instrucoes, video_url,
                       fonte_referencia, ativo
                FROM exercicio
                WHERE id = ?
                """;

        return SqliteSupport.executar(
                conexao,
                "buscar exercício por ID",
                connection -> {

                    try (PreparedStatement ps =
                                 connection.prepareStatement(sql)) {

                        ps.setString(1, id.toString());

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
    public List<Exercicio> listarAtivos() {

        String sql = """
                SELECT id, nome, instrucoes, video_url,
                       fonte_referencia, ativo
                FROM exercicio
                WHERE ativo = 1
                ORDER BY nome COLLATE NOCASE
                """;

        return SqliteSupport.executar(
                conexao,
                "listar exercícios ativos",
                connection -> {

                    List<Exercicio> resultado = new ArrayList<>();

                    try (PreparedStatement ps =
                                 connection.prepareStatement(sql);
                         ResultSet rs = ps.executeQuery()) {

                        while (rs.next()) {
                            resultado.add(mapear(rs));
                        }
                    }

                    return List.copyOf(resultado);
                }
        );
    }

    private Exercicio mapear(ResultSet rs)
            throws java.sql.SQLException {

        return new Exercicio(
                UUID.fromString(rs.getString("id")),
                rs.getString("nome"),
                rs.getString("instrucoes"),
                rs.getString("video_url"),
                rs.getString("fonte_referencia"),
                rs.getInt("ativo") == 1
        );
    }
}
