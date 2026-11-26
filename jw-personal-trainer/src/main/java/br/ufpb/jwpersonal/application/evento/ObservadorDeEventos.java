
package br.ufpb.jwpersonal.application.evento;

/**
 * Contrato dos observadores de eventos do sistema.
 */
@FunctionalInterface
public interface ObservadorDeEventos {

    /**
     * Chamado quando um evento é publicado.
     */
    void aoReceber(PlanoAtribuidoEvento evento);
}
