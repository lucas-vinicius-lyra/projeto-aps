package br.ufpb.jwpersonal.ui;

import br.ufpb.jwpersonal.domain.excecao.RegraDeNegocioException;

import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;


final class Acoes {

    private static final Logger LOGGER = Logger.getLogger(Acoes.class.getName());

    private Acoes() {
    }

    static void executar(PrintStream saida, Runnable acao) {
        try {
            acao.run();
        } catch (EntradaConsole.EntradaEncerradaException e) {
            throw e;
        } catch (RegraDeNegocioException e) {
            saida.println("Erro: " + e.getMessage());
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Falha inesperada na operação.", e);
            saida.println("Erro inesperado: a operação não foi concluída.");
        }
    }
}