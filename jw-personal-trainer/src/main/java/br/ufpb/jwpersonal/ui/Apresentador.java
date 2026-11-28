package br.ufpb.jwpersonal.ui;

import br.ufpb.jwpersonal.application.facade.JWPersonalTrainerFacade;
import br.ufpb.jwpersonal.application.service.SessaoUsuario;
import br.ufpb.jwpersonal.domain.model.*;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.List;

final class Apresentador {

    private final JWPersonalTrainerFacade facade;
    private final SessaoUsuario sessao;
    private final PrintStream saida;

    Apresentador(JWPersonalTrainerFacade facade, SessaoUsuario sessao, PrintStream saida) {
        this.facade = facade;
        this.sessao = sessao;
        this.saida = saida;
    }

    static String numero(BigDecimal valor) {
        return valor.stripTrailingZeros().toPlainString().replace('.', ',');
    }

    String nomeExercicio(ItemTreino item) {
        Exercicio exercicio = facade.consultarExercicio(sessao, item.exercicioId());

        return exercicio.isAtivo()
                ? exercicio.getNome()
                : exercicio.getNome() + " (inativo)";
    }

    String descrever(ItemTreino item) {
        return nomeExercicio(item) + " - "
                + item.series().valor() + " séries x "
                + item.repeticoes().valor() + " repetições, carga "
                + numero(item.carga().valor()) + " kg";
    }

    void itens(List<ItemTreino> itens) {
        if (itens.isEmpty()) {
            saida.println("     (sem exercícios)");
        }

        for (int i = 0; i < itens.size(); i++) {
            saida.println("     " + (i + 1) + ") " + descrever(itens.get(i)));
        }
    }

    void treinos(List<Treino> treinos) {
        if (treinos.isEmpty()) {
            saida.println("  (sem treinos)");
        }

        for (Treino treino : treinos) {
            saida.println("  " + treino.getNome());
            itens(treino.getItens());
        }
    }

    void versao(VersaoPlano versao) {
        saida.println("Versão " + versao.getNumero() + " [" + versao.getEstado() + "]");
        treinos(versao.getTreinos());
    }

    /** Instruções, referência e vídeo de um exercício. */
    void apoio(Exercicio exercicio) {
        saida.println(exercicio.getNome() + (exercicio.isAtivo() ? "" : " (inativo)"));
        saida.println("     Instruções: " + ouAusente(exercicio.getInstrucoes()));
        saida.println("     Referência: " + ouAusente(exercicio.getFonteReferencia()));
        saida.println("     Vídeo: " + ouAusente(exercicio.getVideoUrl()));
    }

    private static String ouAusente(String valor) {
        return valor == null ? "não cadastrado" : valor;
    }
}