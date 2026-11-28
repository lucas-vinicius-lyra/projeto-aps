package br.ufpb.jwpersonal.domain.politica;

import br.ufpb.jwpersonal.domain.excecao.ValorInvalidoException;
import br.ufpb.jwpersonal.domain.model.Carga;
import br.ufpb.jwpersonal.domain.model.ItemTreino;
import br.ufpb.jwpersonal.domain.model.Repeticoes;

import java.math.BigDecimal;
import java.util.Objects;

public final class ProgressaoDupla implements PoliticaProgressao {

    public static final int REPETICOES_MINIMAS_PADRAO = 8;
    public static final int REPETICOES_MAXIMAS_PADRAO = 12;
    public static final int INCREMENTO_REPETICOES_PADRAO = 1;
    public static final BigDecimal PERCENTUAL_CARGA_PADRAO = new BigDecimal("5");

    private static final BigDecimal CARGA_MAXIMA = new BigDecimal("500");
    private static final BigDecimal PASSO_MINIMO = new BigDecimal("0.5");
    private static final BigDecimal CEM = new BigDecimal("100");

    private final int repeticoesMinimas;
    private final int repeticoesMaximas;
    private final int incrementoRepeticoes;
    private final BigDecimal percentualCarga;

    public ProgressaoDupla() {
        this(REPETICOES_MINIMAS_PADRAO, REPETICOES_MAXIMAS_PADRAO,
                INCREMENTO_REPETICOES_PADRAO, PERCENTUAL_CARGA_PADRAO);
    }

    public ProgressaoDupla(int repeticoesMinimas, int repeticoesMaximas,
                           int incrementoRepeticoes, BigDecimal percentualCarga) {

        new Repeticoes(repeticoesMinimas);
        new Repeticoes(repeticoesMaximas);

        if (repeticoesMinimas >= repeticoesMaximas) {
            throw new ValorInvalidoException(
                    "O limite inferior de repetições deve ser menor que o superior.");
        }

        if (incrementoRepeticoes < 1) {
            throw new ValorInvalidoException(
                    "O incremento de repetições deve ser de pelo menos 1.");
        }

        Objects.requireNonNull(percentualCarga, "O percentual é obrigatório.");

        if (percentualCarga.signum() <= 0) {
            throw new ValorInvalidoException(
                    "O percentual de aumento de carga deve ser positivo.");
        }

        this.repeticoesMinimas = repeticoesMinimas;
        this.repeticoesMaximas = repeticoesMaximas;
        this.incrementoRepeticoes = incrementoRepeticoes;
        this.percentualCarga = percentualCarga;
    }

    @Override
    public ItemTreino progredir(ItemTreino item) {

        Objects.requireNonNull(item, "O item de treino é obrigatório.");

        int atuais = item.repeticoes().valor();

        if (atuais < repeticoesMaximas) {
            int novas = (int) Math.min((long) atuais + incrementoRepeticoes, repeticoesMaximas);
            return item.comRepeticoes(new Repeticoes(novas));
        }

        Carga novaCarga = aumentar(item.carga());

        if (novaCarga.valor().compareTo(item.carga().valor()) <= 0) {
            return item;
        }

        return item.comCarga(novaCarga)
                .comRepeticoes(new Repeticoes(repeticoesMinimas));
    }

    private Carga aumentar(Carga atual) {

        BigDecimal valor = atual.valor();

        if (valor.signum() == 0 || valor.compareTo(CARGA_MAXIMA) >= 0) {
            return atual;
        }

        BigDecimal semArredondar = valor.add(
                valor.multiply(percentualCarga).divide(CEM));

        if (semArredondar.compareTo(CARGA_MAXIMA) >= 0) {
            return new Carga(CARGA_MAXIMA);
        }

        Carga arredondada = atual.aumentarPercentual(percentualCarga);

        if (arredondada.valor().compareTo(valor) > 0) {
            return arredondada;
        }

        return new Carga(valor.add(PASSO_MINIMO).min(CARGA_MAXIMA));
    }

    public int getRepeticoesMinimas() {
        return repeticoesMinimas;
    }

    public int getRepeticoesMaximas() {
        return repeticoesMaximas;
    }

    public int getIncrementoRepeticoes() {
        return incrementoRepeticoes;
    }

    public BigDecimal getPercentualCarga() {
        return percentualCarga;
    }
}