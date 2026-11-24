
package br.ufpb.jwpersonal.infra.sqlite;

import br.ufpb.jwpersonal.domain.model.*;
import br.ufpb.jwpersonal.repository.PlanoRepository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistência SQLite de planos e todo o seu histórico.
 */
public class SqlitePlanoRepository implements PlanoRepository {

    private final ConexaoSQLite conexao;

    public SqlitePlanoRepository(ConexaoSQLite conexao) {
        this.conexao = Objects.requireNonNull(conexao);
    }

    @Override
    public void salvar(PlanoTreino plano) {

        Objects.requireNonNull(plano);

        SqliteSupport.executar(
                conexao,
                "salvar plano completo",
                connection -> {

                    salvarDadosPlano(connection, plano);

                    return null;
                }
        );
    }

    private void salvarDadosPlano(
            java.sql.Connection connection,
            PlanoTreino plano) throws SQLException {

        // 1. Salvar os dados principais do plano.
        String sqlPlano = """
                INSERT INTO plano (
                    id, titulo, objetivo, personal_id
                )
                VALUES (?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    titulo = excluded.titulo,
                    objetivo = excluded.objetivo,
                    personal_id = excluded.personal_id
                """;

        try (PreparedStatement ps =
                     connection.prepareStatement(sqlPlano)) {

            ps.setString(1, plano.getId().toString());
            ps.setString(2, plano.getTitulo());
            ps.setString(3, plano.getObjetivo().name());
            ps.setString(4, plano.getPersonalId().toString());

            ps.executeUpdate();
        }

        // 2. Salvar cada versão do plano.
        for (VersaoPlano versao : plano.historico()) {

            salvarVersao(
                    connection,
                    plano.getId(),
                    versao
            );
        }
    }

    private void salvarVersao(
            java.sql.Connection connection,
            UUID planoId,
            VersaoPlano versao) throws SQLException {

        String sqlVersao = """
                INSERT INTO versao_plano (
                    plano_id, numero, estado,
                    criada_em, publicada_em
                )
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(plano_id, numero) DO UPDATE SET
                    estado = excluded.estado,
                    criada_em = excluded.criada_em,
                    publicada_em = excluded.publicada_em
                """;

        try (PreparedStatement ps =
                     connection.prepareStatement(sqlVersao)) {

            ps.setString(1, planoId.toString());
            ps.setInt(2, versao.getNumero());
            ps.setString(3, versao.getEstado().name());
            ps.setString(4, versao.getCriadaEm().toString());

            if (versao.getPublicadaEm() == null) {
                ps.setNull(5, java.sql.Types.VARCHAR);
            } else {
                ps.setString(5, versao.getPublicadaEm().toString());
            }

            ps.executeUpdate();
        }

        // Substitui os treinos e itens da versão pelo conteúdo atual
        // do agregado. As atribuições e execuções não são removidas.
        String removerTreinos = """
                DELETE FROM treino
                WHERE plano_id = ? AND numero_versao = ?
                """;

        try (PreparedStatement ps =
                     connection.prepareStatement(removerTreinos)) {

            ps.setString(1, planoId.toString());
            ps.setInt(2, versao.getNumero());

            ps.executeUpdate();
        }

        int ordem = 0;

        for (Treino treino : versao.getTreinos()) {

            salvarTreino(
                    connection,
                    planoId,
                    versao.getNumero(),
                    treino,
                    ordem++
            );
        }
    }

    private void salvarTreino(
            java.sql.Connection connection,
            UUID planoId,
            int numeroVersao,
            Treino treino,
            int ordem) throws SQLException {

        String sqlTreino = """
                INSERT INTO treino (
                    plano_id, numero_versao, nome, ordem
                )
                VALUES (?, ?, ?, ?)
                """;

        try (PreparedStatement ps =
                     connection.prepareStatement(sqlTreino)) {

            ps.setString(1, planoId.toString());
            ps.setInt(2, numeroVersao);
            ps.setString(3, treino.getNome());
            ps.setInt(4, ordem);

            ps.executeUpdate();
        }

        String sqlItem = """
                INSERT INTO item_treino (
                    plano_id, numero_versao, nome_treino,
                    ordem, exercicio_id, series,
                    repeticoes, carga
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        int ordemItem = 0;

        for (ItemTreino item : treino.getItens()) {

            try (PreparedStatement ps =
                         connection.prepareStatement(sqlItem)) {

                ps.setString(1, planoId.toString());
                ps.setInt(2, numeroVersao);
                ps.setString(3, treino.getNome());
                ps.setInt(4, ordemItem++);
                ps.setString(5, item.exercicioId().toString());
                ps.setInt(6, item.series().valor());
                ps.setInt(7, item.repeticoes().valor());
                ps.setString(8, item.carga().valor().toPlainString());

                ps.executeUpdate();
            }
        }
    }

    @Override
    public Optional<PlanoTreino> buscarPorId(UUID id) {

        Objects.requireNonNull(id);

        return SqliteSupport.executar(
                conexao,
                "buscar plano por ID",
                connection -> buscarPlano(connection, id)
        );
    }

    @Override
    public List<PlanoTreino> listarPorPersonal(UUID personalId) {

        Objects.requireNonNull(personalId);

        return SqliteSupport.executar(
                conexao,
                "listar planos do personal",
                connection -> {

                    List<PlanoTreino> resultado = new ArrayList<>();

                    String sql = """
                            SELECT id
                            FROM plano
                            WHERE personal_id = ?
                            ORDER BY titulo COLLATE NOCASE
                            """;

                    try (PreparedStatement ps =
                                 connection.prepareStatement(sql)) {

                        ps.setString(1, personalId.toString());

                        List<UUID> ids = new ArrayList<>();

                        try (ResultSet rs = ps.executeQuery()) {

                            while (rs.next()) {
                                ids.add(UUID.fromString(
                                        rs.getString("id")
                                ));
                            }
                        }

                        for (UUID id : ids) {
                            buscarPlano(connection, id)
                                    .ifPresent(resultado::add);
                        }
                    }

                    return List.copyOf(resultado);
                }
        );
    }

    private Optional<PlanoTreino> buscarPlano(
            java.sql.Connection connection,
            UUID id) throws SQLException {

        String sql = """
                SELECT id, titulo, objetivo, personal_id
                FROM plano
                WHERE id = ?
                """;

        String titulo;
        ObjetivoPlano objetivo;
        UUID personalId;

        try (PreparedStatement ps =
                     connection.prepareStatement(sql)) {

            ps.setString(1, id.toString());

            try (ResultSet rs = ps.executeQuery()) {

                if (!rs.next()) {
                    return Optional.empty();
                }

                titulo = rs.getString("titulo");
                objetivo = ObjetivoPlano.valueOf(
                        rs.getString("objetivo")
                );
                personalId = UUID.fromString(
                        rs.getString("personal_id")
                );
            }
        }

        List<VersaoPlano> versoes =
                carregarVersoes(connection, id);

        return Optional.of(
                new PlanoTreino(
                        id,
                        titulo,
                        objetivo,
                        personalId,
                        versoes
                )
        );
    }

    private List<VersaoPlano> carregarVersoes(
            java.sql.Connection connection,
            UUID planoId) throws SQLException {

        List<DadosVersao> dados = new ArrayList<>();

        String sql = """
                SELECT numero, estado, criada_em, publicada_em
                FROM versao_plano
                WHERE plano_id = ?
                ORDER BY numero
                """;

        try (PreparedStatement ps =
                     connection.prepareStatement(sql)) {

            ps.setString(1, planoId.toString());

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    String publicadaEm =
                            rs.getString("publicada_em");

                    dados.add(new DadosVersao(
                            rs.getInt("numero"),
                            EstadoVersao.valueOf(
                                    rs.getString("estado")
                            ),
                            java.time.LocalDateTime.parse(
                                    rs.getString("criada_em")
                            ),
                            publicadaEm == null
                                    ? null
                                    : java.time.LocalDateTime.parse(
                                    publicadaEm
                            )
                    ));
                }
            }
        }

        List<VersaoPlano> resultado = new ArrayList<>();

        for (DadosVersao dado : dados) {

            List<Treino> treinos = carregarTreinos(
                    connection,
                    planoId,
                    dado.numero()
            );

            resultado.add(new VersaoPlano(
                    dado.numero(),
                    dado.estado(),
                    treinos,
                    dado.criadaEm(),
                    dado.publicadaEm()
            ));
        }

        return List.copyOf(resultado);
    }

    private List<Treino> carregarTreinos(
            java.sql.Connection connection,
            UUID planoId,
            int numeroVersao) throws SQLException {

        List<String> nomes = new ArrayList<>();

        String sql = """
                SELECT nome
                FROM treino
                WHERE plano_id = ? AND numero_versao = ?
                ORDER BY ordem
                """;

        try (PreparedStatement ps =
                     connection.prepareStatement(sql)) {

            ps.setString(1, planoId.toString());
            ps.setInt(2, numeroVersao);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    nomes.add(rs.getString("nome"));
                }
            }
        }

        List<Treino> resultado = new ArrayList<>();

        for (String nome : nomes) {

            List<ItemTreino> itens = carregarItens(
                    connection,
                    planoId,
                    numeroVersao,
                    nome
            );

            resultado.add(new Treino(nome, itens));
        }

        return List.copyOf(resultado);
    }

    private List<ItemTreino> carregarItens(
            java.sql.Connection connection,
            UUID planoId,
            int numeroVersao,
            String nomeTreino) throws SQLException {

        List<ItemTreino> itens = new ArrayList<>();

        String sql = """
                SELECT exercicio_id, series, repeticoes, carga
                FROM item_treino
                WHERE plano_id = ?
                  AND numero_versao = ?
                  AND nome_treino = ?
                ORDER BY ordem
                """;

        try (PreparedStatement ps =
                     connection.prepareStatement(sql)) {

            ps.setString(1, planoId.toString());
            ps.setInt(2, numeroVersao);
            ps.setString(3, nomeTreino);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    itens.add(new ItemTreino(
                            UUID.fromString(
                                    rs.getString("exercicio_id")
                            ),
                            new Series(rs.getInt("series")),
                            new Repeticoes(
                                    rs.getInt("repeticoes")
                            ),
                            new Carga(new BigDecimal(
                                    rs.getString("carga")
                            ))
                    ));
                }
            }
        }

        return List.copyOf(itens);
    }

    private record DadosVersao(
            int numero,
            EstadoVersao estado,
            java.time.LocalDateTime criadaEm,
            java.time.LocalDateTime publicadaEm
    ) {
    }
}
