
package br.ufpb.jwpersonal.application.evento;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Observador responsável por registrar as notificações dos alunos.
 *
 * As notificações ficam separadas por aluno.
 */
public class NotificadorAluno implements ObservadorDeEventos {

    private final ConcurrentHashMap<UUID,
            CopyOnWriteArrayList<PlanoAtribuidoEvento>>
            notificacoes = new ConcurrentHashMap<>();

    /**
     * Recebe o evento e registra a notificação
     * somente para o aluno destinatário.
     */
    @Override
    public void aoReceber(PlanoAtribuidoEvento evento) {

        Objects.requireNonNull(
                evento,
                "O evento é obrigatório."
        );

        notificacoes
                .computeIfAbsent(
                        evento.alunoId(),
                        id -> new CopyOnWriteArrayList<>()
                )
                .add(evento);
    }

    /**
     * Consulta as notificações de determinado aluno.
     */
    public List<PlanoAtribuidoEvento> notificacoesDoAluno(
            UUID alunoId) {

        Objects.requireNonNull(alunoId);

        List<PlanoAtribuidoEvento> resultado =
                notificacoes.get(alunoId);

        if (resultado == null) {
            return List.of();
        }

        return List.copyOf(resultado);
    }

    /**
     * Remove as notificações armazenadas para determinado aluno.
     */
    public void limparNotificacoes(UUID alunoId) {

        Objects.requireNonNull(alunoId);

        notificacoes.remove(alunoId);
    }
}
