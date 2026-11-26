
package br.ufpb.jwpersonal.application.evento;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Publica eventos para os observadores inscritos.
 */
public class PublicadorDeEventos {

    private static final Logger LOGGER =
            Logger.getLogger(PublicadorDeEventos.class.getName());

    private final CopyOnWriteArrayList<ObservadorDeEventos>
            observadores = new CopyOnWriteArrayList<>();

    /**
     * Inscreve um observador.
     *
     * Não permite inscrições duplicadas da mesma instância.
     */
    public void inscrever(ObservadorDeEventos observador) {

        Objects.requireNonNull(
                observador,
                "O observador é obrigatório."
        );

        observadores.addIfAbsent(observador);
    }

    /**
     * Remove um observador.
     */
    public void remover(ObservadorDeEventos observador) {

        if (observador != null) {
            observadores.remove(observador);
        }
    }

    /**
     * Publica o evento para todos os observadores.
     *
     * Uma falha em um observador não impede a comunicação
     * com os demais e não desfaz uma atribuição já confirmada.
     */
    public void publicar(PlanoAtribuidoEvento evento) {

        Objects.requireNonNull(
                evento,
                "O evento é obrigatório."
        );

        for (ObservadorDeEventos observador : observadores) {

            try {

                observador.aoReceber(evento);

            } catch (RuntimeException erro) {

                LOGGER.log(
                        Level.WARNING,
                        "Falha ao notificar um observador do evento de atribuição.",
                        erro
                );
            }
        }
    }

    /**
     * Retorna a quantidade de observadores inscritos.
     */
    public int quantidadeObservadores() {
        return observadores.size();
    }
}
