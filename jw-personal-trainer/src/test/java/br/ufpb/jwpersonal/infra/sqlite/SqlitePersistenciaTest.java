
package br.ufpb.jwpersonal.infra.sqlite;

import br.ufpb.jwpersonal.domain.model.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de integração da persistência SQLite.
 *
 * Cada teste utiliza um arquivo temporário independente.
 */
class SqlitePersistenciaTest {

    @TempDir
    Path diretorioTemporario;

    private Path arquivoBanco;

    private ConexaoSQLite conexao;

    private SqliteUsuarioRepository usuarios;
    private SqliteExercicioRepository exercicios;
    private SqlitePlanoRepository planos;
    private SqliteAtribuicaoRepository atribuicoes;
    private SqliteExecucaoRepository execucoes;
    private SqliteUnidadeDeTrabalho unidade;

    @BeforeEach
    void prepararBanco() {

        arquivoBanco = diretorioTemporario.resolve("teste.db");

        conexao = new ConexaoSQLite(arquivoBanco);

        SqliteSchema.criarTabelas(conexao);

        usuarios = new SqliteUsuarioRepository(conexao);
        exercicios = new SqliteExercicioRepository(conexao);
        planos = new SqlitePlanoRepository(conexao);
        atribuicoes = new SqliteAtribuicaoRepository(conexao);
        execucoes = new SqliteExecucaoRepository(conexao);

        unidade = new SqliteUnidadeDeTrabalho(conexao);
    }

    /**
     * Testa se os dados do plano e seus relacionamentos
     * continuam disponíveis após abrir uma nova conexão.
     */
    @Test
    void deveRecuperarPlanoCompletoAposReabrirBanco() {

        Cenario cenario = criarCenario();

        unidade.executar(() -> {
            planos.salvar(cenario.plano());
            return null;
        });

        // Simula o encerramento e a reabertura da aplicação.
        ConexaoSQLite novaConexao =
                new ConexaoSQLite(arquivoBanco);

        SqlitePlanoRepository novoRepositorio =
                new SqlitePlanoRepository(novaConexao);

        PlanoTreino recuperado = novoRepositorio
                .buscarPorId(cenario.plano().getId())
                .orElseThrow();

        assertEquals(
                cenario.plano().getTitulo(),
                recuperado.getTitulo()
        );

        assertEquals(
                ObjetivoPlano.HIPERTROFIA,
                recuperado.getObjetivo()
        );

        assertEquals(1, recuperado.historico().size());

        VersaoPlano versao = recuperado.versaoPorNumero(1);

        assertEquals(
                EstadoVersao.PUBLICADA,
                versao.getEstado()
        );

        assertEquals(1, versao.getTreinos().size());

        Treino treino = versao.getTreinos().get(0);

        assertEquals("Treino A", treino.getNome());

        assertEquals(1, treino.getItens().size());

        ItemTreino item = treino.getItens().get(0);

        assertEquals(
                0,
                new BigDecimal("40").compareTo(item.carga().valor()),
                "A carga recuperada deve ser numericamente igual a 40 kg."
        );

        assertEquals(
                3,
                item.series().valor()
        );

        assertEquals(
                12,
                item.repeticoes().valor()
        );
    }

    /**
     * Testa a operação atômica de substituição de atribuição.
     *
     * Se ocorrer uma falha depois do encerramento da atribuição
     * anterior e da tentativa de criação da nova, o rollback
     * deverá restaurar a situação inicial.
     */
    @Test
    void deveRestaurarAtribuicaoAnteriorQuandoOperacaoFalhar() {

        Cenario cenario = criarCenario();

        unidade.executar(() -> {
            planos.salvar(cenario.plano());
            return null;
        });

        Atribuicao anterior = new Atribuicao(
                cenario.aluno().getId(),
                cenario.plano().getId(),
                1,
                LocalDate.now().minusDays(5)
        );

        atribuicoes.salvar(anterior);

        assertThrows(
                IllegalStateException.class,
                () -> unidade.executar(() -> {

                    Atribuicao atual = atribuicoes
                            .buscarAtivaDoAluno(
                                    cenario.aluno().getId()
                            )
                            .orElseThrow();

                    atual.encerrar(LocalDate.now());

                    atribuicoes.salvar(atual);

                    Atribuicao nova = new Atribuicao(
                            cenario.aluno().getId(),
                            cenario.plano().getId(),
                            1,
                            LocalDate.now()
                    );

                    atribuicoes.salvar(nova);

                    throw new IllegalStateException(
                            "Falha simulada durante a operação."
                    );
                })
        );

        Atribuicao restaurada = atribuicoes
                .buscarAtivaDoAluno(cenario.aluno().getId())
                .orElseThrow();

        assertEquals(
                anterior.getId(),
                restaurada.getId()
        );

        assertEquals(
                StatusAtribuicao.ATIVA,
                restaurada.getStatus()
        );

        assertNull(restaurada.getFim());

        assertEquals(
                1,
                atribuicoes.historicoDoAluno(
                        cenario.aluno().getId()
                ).size()
        );
    }

    /**
     * Testa a restrição UNIQUE da chave natural da execução.
     */
    @Test
    void naoDevePermitirExecucoesDuplicadas() {

        Cenario cenario = criarCenario();

        unidade.executar(() -> {

            planos.salvar(cenario.plano());

            atribuicoes.salvar(new Atribuicao(
                    cenario.aluno().getId(),
                    cenario.plano().getId(),
                    1,
                    LocalDate.now()
            ));

            return null;
        });

        Atribuicao atribuicao = atribuicoes
                .buscarAtivaDoAluno(cenario.aluno().getId())
                .orElseThrow();

        Execucao primeira = new Execucao(
                cenario.aluno().getId(),
                atribuicao.getId(),
                cenario.plano().getId(),
                1,
                "Treino A",
                LocalDate.now()
        );

        execucoes.salvar(primeira);

        assertTrue(
                execucoes.existePorChaveNatural(
                        primeira.chaveNatural()
                )
        );

        Execucao duplicada = new Execucao(
                cenario.aluno().getId(),
                atribuicao.getId(),
                cenario.plano().getId(),
                1,
                "Treino A",
                LocalDate.now()
        );

        assertThrows(
                PersistenciaSQLiteException.class,
                () -> execucoes.salvar(duplicada)
        );

        assertEquals(
                1,
                execucoes.historicoDoAluno(
                        cenario.aluno().getId()
                ).size()
        );
    }

    /**
     * Testa se o banco impede referências a exercícios inexistentes.
     */
    @Test
    void deveImpedirItemComExercicioInexistente() {

        Cenario cenario = criarCenario();

        UUID exercicioInexistente = UUID.randomUUID();

        PlanoTreino planoInvalido = new PlanoTreino(
                "Plano inválido",
                ObjetivoPlano.HIPERTROFIA,
                cenario.personal().getId()
        );

        Treino treino = new Treino("Treino X");

        treino.adicionarItem(new ItemTreino(
                exercicioInexistente,
                new Series(3),
                new Repeticoes(10),
                new Carga(new BigDecimal("20"))
        ));

        planoInvalido.versaoPorNumero(1)
                .adicionarTreino(treino);

        planoInvalido.publicar(1);

        // O personal existe, mas o exercício informado não.
        assertThrows(
                PersistenciaSQLiteException.class,
                () -> unidade.executar(() -> {

                    planos.salvar(planoInvalido);

                    return null;
                })
        );

        // A transação deve ter sido desfeita.
        assertTrue(
                planos.buscarPorId(planoInvalido.getId()).isEmpty()
        );
    }

    /**
     * Prepara usuários, exercício e plano para os testes.
     */
    private Cenario criarCenario() {

        Usuario personal = new Usuario(
                "Personal Teste",
                "personal@teste.com",
                "hash-personal",
                Perfil.PERSONAL
        );

        Usuario aluno = new Usuario(
                "Aluno Teste",
                "aluno@teste.com",
                "hash-aluno",
                Perfil.ALUNO
        );

        Exercicio exercicio = new Exercicio(
                "Agachamento",
                "Execução controlada.",
                null,
                "Referência de teste"
        );

        unidade.executar(() -> {

            usuarios.salvar(personal);
            usuarios.salvar(aluno);

            exercicios.salvar(exercicio);

            return null;
        });

        PlanoTreino plano = new PlanoTreino(
                "Plano de Hipertrofia",
                ObjetivoPlano.HIPERTROFIA,
                personal.getId()
        );

        Treino treino = new Treino("Treino A");

        treino.adicionarItem(new ItemTreino(
                exercicio.getId(),
                new Series(3),
                new Repeticoes(12),
                new Carga(new BigDecimal("40"))
        ));

        plano.versaoPorNumero(1).adicionarTreino(treino);

        plano.publicar(1);

        return new Cenario(personal, aluno, plano);
    }

    private record Cenario(
            Usuario personal,
            Usuario aluno,
            PlanoTreino plano
    ) {
    }
}
