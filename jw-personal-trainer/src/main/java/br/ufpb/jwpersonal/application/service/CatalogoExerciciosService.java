package br.ufpb.jwpersonal.application.service;

import br.ufpb.jwpersonal.domain.excecao.RecursoNaoEncontradoException;
import br.ufpb.jwpersonal.domain.model.Exercicio;
import br.ufpb.jwpersonal.repository.ExercicioRepository;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class CatalogoExerciciosService {

    private final ExercicioRepository repositorio;
    private final Autorizador autorizador;

    public CatalogoExerciciosService(
            ExercicioRepository repositorio,
            Autorizador autorizador) {

        this.repositorio = Objects.requireNonNull(repositorio);
        this.autorizador = Objects.requireNonNull(autorizador);
    }

    // Cadastrar um novo exercício
    public Exercicio cadastrar(
            SessaoUsuario sessao,
            String nome,
            String instrucoes,
            String videoUrl,
            String fonte) {

        autorizador.exigirPersonal(sessao);

        Exercicio exercicio = new Exercicio(
                nome, instrucoes, videoUrl, fonte
        );

        repositorio.salvar(exercicio);

        return exercicio;
    }

    // Atualizar as informações do exercício
    public Exercicio atualizarConteudo(
            SessaoUsuario sessao,
            UUID exercicioId,
            String instrucoes,
            String videoUrl,
            String fonte) {

        autorizador.exigirPersonal(sessao);

        Exercicio exercicio = buscar(exercicioId);

        exercicio.atualizarConteudoApoio(
                instrucoes, videoUrl, fonte
        );

        repositorio.salvar(exercicio);

        return exercicio;
    }

    // Inativar sem excluir do histórico
    public void inativar(
            SessaoUsuario sessao,
            UUID exercicioId) {

        autorizador.exigirPersonal(sessao);

        Exercicio exercicio = buscar(exercicioId);

        exercicio.inativar();

        repositorio.salvar(exercicio);
    }

    // Listar os exercícios disponíveis
    public List<Exercicio> listarAtivos(
            SessaoUsuario sessao) {

        autorizador.exigirAutenticado(sessao);

        return repositorio.listarAtivos();
    }

    // Método auxiliar
    private Exercicio buscar(UUID id) {

        return repositorio.buscarPorId(id)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "Exercício não encontrado."
                        )
                );
    }
}
