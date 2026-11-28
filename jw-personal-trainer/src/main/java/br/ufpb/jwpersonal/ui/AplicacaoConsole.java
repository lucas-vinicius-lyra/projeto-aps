package br.ufpb.jwpersonal.ui;

import br.ufpb.jwpersonal.application.facade.JWPersonalTrainerFacade;
import br.ufpb.jwpersonal.application.service.SessaoUsuario;

import java.io.PrintStream;
import java.util.Objects;
import java.util.Scanner;

public final class AplicacaoConsole {

    private final JWPersonalTrainerFacade facade;
    private final EntradaConsole entrada;
    private final PrintStream saida;

    public AplicacaoConsole(JWPersonalTrainerFacade facade,
                            Scanner scanner, PrintStream saida) {
        this.facade = Objects.requireNonNull(facade);
        this.entrada = new EntradaConsole(scanner, saida);
        this.saida = saida;
    }

    public void iniciar() {
        try {
            while (true) {
                saida.println();
                saida.println("=== JW PERSONAL TRAINER ===");
                saida.println("1. Entrar no sistema");
                saida.println("2. Sair");

                int opcao = entrada.lerOpcao(2);

                if (opcao == 2) {
                    break;
                }

                if (opcao == 1) {
                    Acoes.executar(saida, this::entrar);
                }
            }
        } catch (EntradaConsole.EntradaEncerradaException e) {
            saida.println();
        }

        saida.println("Até logo!");
    }

    private void entrar() {
        SessaoUsuario sessao = facade.autenticar(
                entrada.lerObrigatorio("E-mail"),
                entrada.lerObrigatorio("Senha"));

        saida.println("Bem-vindo(a), " + sessao.nome() + "!");

        if (sessao.ehPersonal()) {
            new MenuPersonal(facade, sessao, entrada).exibir();
        } else {
            new MenuAluno(facade, sessao, entrada).exibir();
        }
    }
}