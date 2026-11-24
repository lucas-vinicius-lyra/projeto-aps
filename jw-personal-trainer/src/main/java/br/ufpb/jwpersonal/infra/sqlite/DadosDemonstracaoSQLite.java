
package br.ufpb.jwpersonal.infra.sqlite;

import br.ufpb.jwpersonal.application.service.SenhaHasher;
import br.ufpb.jwpersonal.domain.model.*;
import br.ufpb.jwpersonal.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.List;

/**
 * Cria dados fictícios para demonstração do JW Personal Trainer.
 *
 * A carga é realizada somente quando o usuário de demonstração
 * do Personal ainda não existe.
 */
public class DadosDemonstracaoSQLite {

    public static final String EMAIL_PERSONAL =
            "personal@jwpersonal.local";

    public static final String SENHA_PERSONAL =
            "Personal123!";

    public static final String EMAIL_ALUNO =
            "aluno@jwpersonal.local";

    public static final String SENHA_ALUNO =
            "Aluno123!";

    private final UsuarioRepository usuarios;
    private final ExercicioRepository exercicios;
    private final PlanoRepository planos;
    private final AtribuicaoRepository atribuicoes;
    private final UnidadeDeTrabalho unidadeDeTrabalho;
    private final SenhaHasher senhaHasher;

    public DadosDemonstracaoSQLite(
            UsuarioRepository usuarios,
            ExercicioRepository exercicios,
            PlanoRepository planos,
            AtribuicaoRepository atribuicoes,
            UnidadeDeTrabalho unidadeDeTrabalho,
            SenhaHasher senhaHasher) {

        this.usuarios = Objects.requireNonNull(usuarios);
        this.exercicios = Objects.requireNonNull(exercicios);
        this.planos = Objects.requireNonNull(planos);
        this.atribuicoes = Objects.requireNonNull(atribuicoes);
        this.unidadeDeTrabalho =
                Objects.requireNonNull(unidadeDeTrabalho);
        this.senhaHasher = Objects.requireNonNull(senhaHasher);
    }

    /**
     * Carrega os dados somente quando ainda não existem.
     */
    public void carregarSeNecessario() {

        if (usuarios.buscarPorEmail(EMAIL_PERSONAL).isPresent()) {
            return;
        }

        Usuario personal = new Usuario(
                "Personal de Demonstração",
                EMAIL_PERSONAL,
                senhaHasher.gerarHash(SENHA_PERSONAL),
                Perfil.PERSONAL
        );

        Usuario aluno = new Usuario(
                "Aluno de Demonstração",
                EMAIL_ALUNO,
                senhaHasher.gerarHash(SENHA_ALUNO),
                Perfil.ALUNO
        );

        Exercicio agachamento = new Exercicio(
                "Agachamento livre",
                "Mantenha a coluna alinhada e flexione os joelhos.",
                null,
                "Referência técnica de demonstração"
        );

        Exercicio supino = new Exercicio(
                "Supino reto",
                "Mantenha os pés apoiados e controle a descida da barra.",
                null,
                "Referência técnica de demonstração"
        );

        PlanoTreino plano = criarPlanoDemo(personal, agachamento, supino);

        Atribuicao atribuicao = new Atribuicao(
                aluno.getId(),
                plano.getId(),
                1,
                LocalDate.now()
        );

        // Tudo é inserido em uma única transação.
        unidadeDeTrabalho.executar(() -> {

            usuarios.salvar(personal);
            usuarios.salvar(aluno);

            exercicios.salvar(agachamento);
            exercicios.salvar(supino);

            planos.salvar(plano);

            atribuicoes.salvar(atribuicao);

            return null;
        });
    }

    private PlanoTreino criarPlanoDemo(
            Usuario personal,
            Exercicio agachamento,
            Exercicio supino) {

        PlanoTreino plano = new PlanoTreino(
                "Plano de Hipertrofia - Demonstração",
                ObjetivoPlano.HIPERTROFIA,
                personal.getId()
        );

        Treino treinoA = new Treino("Treino A");

        treinoA.adicionarItem(new ItemTreino(
                agachamento.getId(),
                new Series(3),
                new Repeticoes(12),
                new Carga(new BigDecimal("40"))
        ));

        treinoA.adicionarItem(new ItemTreino(
                supino.getId(),
                new Series(3),
                new Repeticoes(10),
                new Carga(new BigDecimal("30"))
        ));

        plano.versaoPorNumero(1).adicionarTreino(treinoA);

        Treino treinoB = new Treino("Treino B");

        treinoB.adicionarItem(new ItemTreino(
                supino.getId(),
                new Series(3),
                new Repeticoes(12),
                new Carga(new BigDecimal("25"))
        ));

        plano.versaoPorNumero(1).adicionarTreino(treinoB);

        // A versão inicial já fica publicada para poder ser atribuída.
        plano.publicar(1);

        return plano;
    }
}
