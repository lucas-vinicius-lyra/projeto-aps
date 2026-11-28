package br.ufpb.jwpersonal.ui;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;
import java.util.function.Function;

final class EntradaConsole {
    static final class EntradaEncerradaException extends RuntimeException {
    }

    static final DateTimeFormatter DATA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu")
                    .withResolverStyle(ResolverStyle.STRICT);

    private final Scanner scanner;
    private final PrintStream saida;

    EntradaConsole(Scanner scanner, PrintStream saida) {
        this.scanner = Objects.requireNonNull(scanner);
        this.saida = Objects.requireNonNull(saida);
    }

    PrintStream saida() {
        return saida;
    }

    String lerTexto(String rotulo) {
        saida.print(rotulo + ": ");
        saida.flush();

        if (!scanner.hasNextLine()) {
            throw new EntradaEncerradaException();
        }

        return scanner.nextLine().trim();
    }

    String lerObrigatorio(String rotulo) {
        while (true) {
            String valor = lerTexto(rotulo);

            if (!valor.isEmpty()) {
                return valor;
            }

            saida.println("Preenchimento obrigatório.");
        }
    }

    int lerInteiro(String rotulo, Integer padrao) {
        while (true) {
            String valor = lerTexto(padrao == null
                    ? rotulo : rotulo + " [" + padrao + "]");

            if (valor.isEmpty() && padrao != null) {
                return padrao;
            }

            try {
                return Integer.parseInt(valor);
            } catch (NumberFormatException e) {
                saida.println("Entrada inválida: digite um número inteiro.");
            }
        }
    }

    BigDecimal lerDecimal(String rotulo, BigDecimal padrao) {
        while (true) {
            String valor = lerTexto(padrao == null
                    ? rotulo : rotulo + " [" + Apresentador.numero(padrao) + "]");

            if (valor.isEmpty() && padrao != null) {
                return padrao;
            }

            if (valor.matches("-?\\d{1,6}([.,]\\d{1,2})?")) {
                return new BigDecimal(valor.replace(',', '.'));
            }

            saida.println("Entrada inválida: use um número, por exemplo 42,5.");
        }
    }

    LocalDate lerData(String rotulo) {
        while (true) {
            String valor = lerTexto(rotulo + " (dd/mm/aaaa, Enter = hoje)");

            if (valor.isEmpty()) {
                return LocalDate.now();
            }

            try {
                return LocalDate.parse(valor, DATA);
            } catch (DateTimeParseException e) {
                saida.println("Data inválida.");
            }
        }
    }

    boolean confirmar(String pergunta) {
        while (true) {
            String valor = lerTexto(pergunta + " (s/n)").toLowerCase();

            if (valor.equals("s")) {
                return true;
            }

            if (valor.equals("n")) {
                return false;
            }

            saida.println("Responda com s ou n.");
        }
    }

    int lerOpcao(int maximo) {
        while (true) {
            String valor = lerTexto("Opção");

            try {
                int opcao = Integer.parseInt(valor);

                if (opcao >= 0 && opcao <= maximo) {
                    return opcao;
                }
            } catch (NumberFormatException e) {
                // cai na mensagem abaixo
            }

            saida.println("Opção inválida. Digite um número de 0 a " + maximo + ".");
        }
    }

    <T> Optional<T> escolher(String titulo, List<T> itens, Function<T, String> rotulo) {
        if (itens.isEmpty()) {
            saida.println(titulo + ": nenhum registro disponível.");
            return Optional.empty();
        }

        saida.println();
        saida.println(titulo);

        for (int i = 0; i < itens.size(); i++) {
            saida.println("  " + (i + 1) + ". " + rotulo.apply(itens.get(i)));
        }

        saida.println("  0. Voltar");

        int opcao = lerOpcao(itens.size());

        return opcao == 0 ? Optional.empty() : Optional.of(itens.get(opcao - 1));
    }
}