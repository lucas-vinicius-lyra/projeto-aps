
package br.ufpb.jwpersonal.infra.sqlite;

import br.ufpb.jwpersonal.domain.model.Perfil;
import br.ufpb.jwpersonal.domain.model.Usuario;
import br.ufpb.jwpersonal.repository.UsuarioRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistência SQLite de usuários.
 */
public class SqliteUsuarioRepository implements UsuarioRepository {

    private final ConexaoSQLite conexao;

    public SqliteUsuarioRepository(ConexaoSQLite conexao) {
        this.conexao = Objects.requireNonNull(conexao);
    }

    @Override
    public void salvar(Usuario usuario) {

        Objects.requireNonNull(usuario);

        String sql = """
                INSERT INTO usuario (
                    id, nome, email, senha_hash, perfil, ativo
                )
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    nome = excluded.nome,
                    email = excluded.email,
                    senha_hash = excluded.senha_hash,
                    perfil = excluded.perfil,
                    ativo = excluded.ativo
                """;

        SqliteSupport.executar(
                conexao,
                "salvar usuário",
                connection -> {

                    try (PreparedStatement ps =
                                 connection.prepareStatement(sql)) {

                        ps.setString(1, usuario.getId().toString());
                        ps.setString(2, usuario.getNome());
                        ps.setString(3, usuario.getEmail());
                        ps.setString(4, usuario.getSenhaHash());
                        ps.setString(5, usuario.getPerfil().name());
                        ps.setInt(6, usuario.isAtivo() ? 1 : 0);

                        ps.executeUpdate();
                    }

                    return null;
                }
        );
    }

    @Override
    public Optional<Usuario> buscarPorId(UUID id) {

        Objects.requireNonNull(id);

        String sql = """
                SELECT id, nome, email, senha_hash, perfil, ativo
                FROM usuario
                WHERE id = ?
                """;

        return SqliteSupport.executar(
                conexao,
                "buscar usuário por ID",
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
    public Optional<Usuario> buscarPorEmail(String email) {

        if (email == null || email.isBlank()) {
            return Optional.empty();
        }

        String sql = """
                SELECT id, nome, email, senha_hash, perfil, ativo
                FROM usuario
                WHERE email = ? COLLATE NOCASE
                """;

        return SqliteSupport.executar(
                conexao,
                "buscar usuário por e-mail",
                connection -> {

                    try (PreparedStatement ps =
                                 connection.prepareStatement(sql)) {

                        ps.setString(1, email.trim());

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

    private Usuario mapear(ResultSet rs) throws java.sql.SQLException {

        return new Usuario(
                UUID.fromString(rs.getString("id")),
                rs.getString("nome"),
                rs.getString("email"),
                rs.getString("senha_hash"),
                Perfil.valueOf(rs.getString("perfil")),
                rs.getInt("ativo") == 1
        );
    }
}
