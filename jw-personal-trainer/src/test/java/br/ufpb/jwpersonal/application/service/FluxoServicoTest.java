
package br.ufpb.jwpersonal.application.service;

import br.ufpb.jwpersonal.domain.fabrica.GeradorDePlano;
import br.ufpb.jwpersonal.domain.fabrica.GeradorEmagrecimento;
import br.ufpb.jwpersonal.domain.fabrica.GeradorHipertrofia;
import br.ufpb.jwpersonal.domain.fabrica.ModeloInicialPlano;
import br.ufpb.jwpersonal.domain.model.*;
import br.ufpb.jwpersonal.repository.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class FluxoServicoTest {

    private final Map<UUID, Usuario> usuarios = new HashMap<>();
    private final Map<UUID, Exercicio> exercicios = new HashMap<>();
    private final Map<UUID, PlanoTreino> planos = new HashMap<>();
    private final Map<UUID, Atribuicao> atribuicoes = new HashMap<>();
    private final Map<UUID, Execucao> execucoes = new HashMap<>();

    private SessaoUsuario personal;
    private SessaoUsuario aluno;

    private CatalogoExerciciosService catalogo;
    private PrescricaoService prescricao;
    private AtribuicaoService atribuicaoService;
    private ExecucaoService execucaoService;
    private ConsultaService consulta;

    @BeforeEach
    void preparar() {

        Usuario usuarioPersonal = new Usuario(
                "Personal",
                "personal@teste.com",
                "hash",
                Perfil.PERSONAL
        );

        Usuario usuarioAluno = new Usuario(
                "Aluno",
                "aluno@teste.com",
                "hash",
                Perfil.ALUNO
        );

        usuarios.put(usuarioPersonal.getId(), usuarioPersonal);
        usuarios.put(usuarioAluno.getId(), usuarioAluno);

        personal = SessaoUsuario.de(usuarioPersonal);
        aluno = SessaoUsuario.de(usuarioAluno);

        Autorizador autorizador = new Autorizador();

        UsuarioRepository usuarioRepo = new UsuarioRepository() {

            public void salvar(Usuario u) {
                usuarios.put(u.getId(), u);
            }

            public Optional<Usuario> buscarPorId(UUID id) {
                return Optional.ofNullable(usuarios.get(id));
            }

            public Optional<Usuario> buscarPorEmail(String email) {
                return usuarios.values().stream()
                        .filter(u -> u.getEmail().equals(email))
                        .findFirst();
            }
        };

        ExercicioRepository exercicioRepo =
                new ExercicioRepository() {

                    public void salvar(Exercicio e) {
                        exercicios.put(e.getId(), e);
                    }

                    public Optional<Exercicio> buscarPorId(UUID id) {
                        return Optional.ofNullable(exercicios.get(id));
                    }

                    public List<Exercicio> listarAtivos() {
                        return exercicios.values().stream()
                                .filter(Exercicio::isAtivo)
                                .toList();
                    }
                };

        PlanoRepository planoRepo = new PlanoRepository() {

            public void salvar(PlanoTreino p) {
                planos.put(p.getId(), p);
            }

            public Optional<PlanoTreino> buscarPorId(UUID id) {
                return Optional.ofNullable(planos.get(id));
            }

            public List<PlanoTreino> listarPorPersonal(UUID id) {
                return planos.values().stream()
                        .filter(p -> p.getPersonalId().equals(id))
                        .toList();
            }
        };

        AtribuicaoRepository atribuicaoRepo =
                new AtribuicaoRepository() {

                    public void salvar(Atribuicao a) {
                        atribuicoes.put(a.getId(), a);
                    }

                    public Optional<Atribuicao> buscarAtivaDoAluno(UUID id) {
                        return atribuicoes.values().stream()
                                .filter(a -> a.getAlunoId().equals(id)
                                        && a.estaAtiva())
                                .findFirst();
                    }

                    public List<Atribuicao> historicoDoAluno(UUID id) {
                        return atribuicoes.values().stream()
                                .filter(a -> a.getAlunoId().equals(id))
                                .toList();
                    }
                };

        ExecucaoRepository execucaoRepo =
                new ExecucaoRepository() {

                    public void salvar(Execucao e) {
                        execucoes.put(e.getId(), e);
                    }

                    public boolean existePorChaveNatural(String chave) {
                        return execucoes.values().stream()
                                .anyMatch(e ->
                                        e.chaveNatural().equals(chave));
                    }

                    public boolean existeParaVersao(UUID id, int numero) {
                        return execucoes.values().stream()
                                .anyMatch(e ->
                                        e.getPlanoId().equals(id)
                                                && e.getNumeroVersao() == numero);
                    }

                    public List<Execucao> historicoDoAluno(UUID id) {
                        return execucoes.values().stream()
                                .filter(e -> e.getAlunoId().equals(id))
                                .toList();
                    }
                };

        // Executor simples para os cenários positivos deste teste.
        UnidadeDeTrabalho unidade = new UnidadeDeTrabalho() {

            public <T> T executar(Supplier<T> operacao) {
                return operacao.get();
            }
        };

        // Registro dos geradores do padrão Factory Method.
        Map<ObjetivoPlano, GeradorDePlano> geradores =
                new EnumMap<>(ObjetivoPlano.class);

        geradores.put(
                ObjetivoPlano.HIPERTROFIA,
                new GeradorHipertrofia()
        );

        geradores.put(
                ObjetivoPlano.EMAGRECIMENTO,
                new GeradorEmagrecimento()
        );

        catalogo = new CatalogoExerciciosService(
                exercicioRepo,
                autorizador
        );

        prescricao = new PrescricaoService(
                planoRepo,
                exercicioRepo,
                execucaoRepo,
                unidade,
                autorizador,
                geradores
        );

        atribuicaoService = new AtribuicaoService(
                atribuicaoRepo,
                planoRepo,
                usuarioRepo,
                unidade,
                autorizador
        );

        execucaoService = new ExecucaoService(
                execucaoRepo,
                atribuicaoRepo,
                planoRepo,
                unidade,
                autorizador
        );

        consulta = new ConsultaService(
                planoRepo,
                atribuicaoRepo,
                execucaoRepo,
                autorizador
        );
    }

    /**
     * Testa a geração do modelo de hipertrofia pela camada de aplicação.
     */
    @Test
    void prescricaoGeraModeloInicialDeHipertrofia() {

        ModeloInicialPlano modelo = prescricao.gerarModeloInicial(
                personal,
                ObjetivoPlano.HIPERTROFIA
        );

        assertEquals(
                ObjetivoPlano.HIPERTROFIA,
                modelo.objetivo()
        );

        assertEquals(
                List.of("Treino A", "Treino B", "Treino C"),
                modelo.nomesTreinos()
        );

        List<Treino> treinos = modelo.criarTreinos();

        assertEquals(3, treinos.size());

        assertTrue(
                treinos.stream().allMatch(
                        treino -> treino.getItens().isEmpty()
                )
        );
    }

    /**
     * Testa a geração do modelo de emagrecimento pela camada de aplicação.
     */
    @Test
    void prescricaoGeraModeloInicialDeEmagrecimento() {

        ModeloInicialPlano modelo = prescricao.gerarModeloInicial(
                personal,
                ObjetivoPlano.EMAGRECIMENTO
        );

        assertEquals(
                ObjetivoPlano.EMAGRECIMENTO,
                modelo.objetivo()
        );

        assertEquals(
                List.of("Treino A", "Treino B"),
                modelo.nomesTreinos()
        );

        List<Treino> treinos = modelo.criarTreinos();

        assertEquals(2, treinos.size());
    }

    private PlanoTreino criarPlanoPublicado() {

        Exercicio exercicio = catalogo.cadastrar(
                personal,
                "Agachamento",
                "Flexionar os joelhos",
                null,
                "Referencia tecnica"
        );

        Treino treino = new Treino("Treino A");

        treino.adicionarItem(new ItemTreino(
                exercicio.getId(),
                new Series(3),
                new Repeticoes(12),
                new Carga(new BigDecimal("20"))
        ));

        PlanoTreino plano = prescricao.prescrever(
                personal,
                "Hipertrofia",
                ObjetivoPlano.HIPERTROFIA,
                List.of(treino)
        );

        prescricao.publicar(
                personal,
                plano.getId(),
                1
        );

        return plano;
    }

    @Test
    void catalogoCadastraExercicio() {

        catalogo.cadastrar(
                personal,
                "Supino",
                "Empurrar a barra",
                null,
                null
        );

        assertEquals(
                1,
                catalogo.listarAtivos(personal).size()
        );
    }

    @Test
    void prescricaoCriaEPublicaPlano() {

        PlanoTreino plano = criarPlanoPublicado();

        assertEquals(
                EstadoVersao.PUBLICADA,
                plano.versaoPorNumero(1).getEstado()
        );
    }

    @Test
    void atribuicaoVinculaPlanoAoAluno() {

        PlanoTreino plano = criarPlanoPublicado();

        Atribuicao atribuicao = atribuicaoService.atribuir(
                personal,
                aluno.usuarioId(),
                plano.getId(),
                1
        );

        assertTrue(atribuicao.estaAtiva());

        assertTrue(
                atribuicaoService
                        .buscarAtribuicaoAtiva(aluno)
                        .isPresent()
        );
    }

    @Test
    void execucaoRepetidaNaoDuplica() {

        PlanoTreino plano = criarPlanoPublicado();

        atribuicaoService.atribuir(
                personal,
                aluno.usuarioId(),
                plano.getId(),
                1
        );

        Execucao primeira = execucaoService.registrarExecucao(
                aluno,
                "Treino A",
                LocalDate.now()
        );

        Execucao segunda = execucaoService.registrarExecucao(
                aluno,
                "Treino A",
                LocalDate.now()
        );

        assertEquals(primeira.getId(), segunda.getId());
        assertEquals(1, execucoes.size());
    }

    @Test
    void consultaRetornaPlanoEHistorico() {

        PlanoTreino plano = criarPlanoPublicado();

        atribuicaoService.atribuir(
                personal,
                aluno.usuarioId(),
                plano.getId(),
                1
        );

        execucaoService.registrarExecucao(
                aluno,
                "Treino A",
                LocalDate.now()
        );

        assertTrue(
                consulta.consultarPlanoAtivo(
                        aluno,
                        aluno.usuarioId()
                ).isPresent()
        );

        assertEquals(
                1,
                consulta.consultarHistoricoExecucoes(
                        aluno,
                        aluno.usuarioId()
                ).size()
        );

        assertEquals(
                1,
                consulta.consultarHistoricoVersoes(
                        aluno,
                        plano.getId()
                ).size()
        );
    }
}
