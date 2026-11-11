package br.ufpb.jwpersonal.domain.model;

import br.ufpb.jwpersonal.domain.excecao.RecursoNaoEncontradoException;
import br.ufpb.jwpersonal.domain.excecao.TransicaoInvalidaException;
import br.ufpb.jwpersonal.domain.excecao.ValorInvalidoException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * Conteúdo de uma versão específica de um plano.
 * A versão publicada é imutável, salvo correção direta autorizada pela RN04
 * quando ainda não existe execução registrada para ela.
 */
public final class VersaoPlano {

    private final int numero;
    private EstadoVersao estado;
    private List<Treino> treinos;
    private final LocalDateTime criadaEm;
    private LocalDateTime publicadaEm;

    public VersaoPlano(int numero) {
        this(
                numero,
                EstadoVersao.RASCUNHO,
                List.of(),
                LocalDateTime.now(),
                null
        );
    }

    public VersaoPlano(int numero, EstadoVersao estado,
                       List<Treino> treinos,
                       LocalDateTime criadaEm,
                       LocalDateTime publicadaEm) {

        if (numero < 1) {
            throw new ValorInvalidoException(
                    "O número da versão deve ser positivo.");
        }

        this.numero = numero;

        this.estado = Objects.requireNonNull(
                estado, "O estado da versão é obrigatório.");

        this.criadaEm = Objects.requireNonNull(
                criadaEm, "A data de criação é obrigatória.");

        this.publicadaEm = publicadaEm;

        Objects.requireNonNull(
                treinos, "A lista de treinos é obrigatória.");

        this.treinos = copiarTreinos(treinos);

        if (estado != EstadoVersao.RASCUNHO) {
            this.treinos.forEach(Treino::bloquearEdicao);
        }

        if (estado == EstadoVersao.RASCUNHO && publicadaEm != null) {
            throw new ValorInvalidoException(
                    "Um rascunho não pode possuir data de publicação.");
        }

        if (estado != EstadoVersao.RASCUNHO && publicadaEm == null) {
            throw new ValorInvalidoException(
                    "Uma versão publicada ou arquivada deve possuir data de publicação.");
        }
    }

    /**
     * Adiciona um treino à versão.
     */
    public void adicionarTreino(Treino treino) {

        verificarSeRascunho();

        Objects.requireNonNull(treino, "O treino é obrigatório.");

        if (treinos.size() >= 7) {
            throw new ValorInvalidoException(
                    "Uma versão pode possuir no máximo 7 treinos.");
        }

        verificarNomeUnico(treino.getNome());

        treinos.add(treino.copiar());
    }

    /**
     * Remove um treino pelo nome.
     */
    public void removerTreino(String nomeTreino) {

        verificarSeRascunho();

        if (nomeTreino == null || nomeTreino.isBlank()) {
            throw new ValorInvalidoException(
                    "Informe o nome do treino que deseja remover.");
        }

        boolean removido = treinos.removeIf(
                treino -> normalizarNome(treino.getNome())
                        .equals(normalizarNome(nomeTreino)));

        if (!removido) {
            throw new RecursoNaoEncontradoException(
                    "Não foi encontrado um treino com esse nome na versão.");
        }
    }

    /**
     * Aplica uma estratégia aos itens de todos os treinos da versão em rascunho.
     */
    public void aplicarProgressao(Function<ItemTreino, ItemTreino> estrategia) {

        verificarSeRascunho();

        Objects.requireNonNull(
                estrategia, "A estratégia de progressão é obrigatória.");

        // Calcula todos os resultados antes de modificar qualquer treino.
        List<List<ItemTreino>> resultados = treinos.stream()
                .map(treino -> treino.calcularItensProgressionados(estrategia))
                .toList();

        for (int i = 0; i < treinos.size(); i++) {
            treinos.get(i).substituirItens(resultados.get(i));
        }
    }

    /**
     * Publica a versão após validar toda a sua estrutura (RN02 e RN07).
     * O arquivamento da versão anterior é coordenado pelo agregado PlanoTreino.
     */
    public void publicar() {

        validarParaPublicacao();

        if (!estado.podeIrPara(EstadoVersao.PUBLICADA)) {
            throw new TransicaoInvalidaException(
                    "Somente uma versão em RASCUNHO pode ser publicada.");
        }

        estado = EstadoVersao.PUBLICADA;
        publicadaEm = LocalDateTime.now();

        treinos.forEach(Treino::bloquearEdicao);
    }

    /**
     * Arquiva uma versão publicada.
     */
    public void arquivar() {

        if (!estado.podeIrPara(EstadoVersao.ARQUIVADA)) {
            throw new TransicaoInvalidaException(
                    "Somente uma versão PUBLICADA pode ser arquivada.");
        }

        estado = EstadoVersao.ARQUIVADA;
    }

    /**
     * Cria uma cópia editável da versão publicada com outro número.
     */
    public VersaoPlano copiarComoRascunho(int novoNumero) {

        if (estado != EstadoVersao.PUBLICADA) {
            throw new TransicaoInvalidaException(
                    "A revisão deve ser iniciada a partir da versão publicada.");
        }

        if (novoNumero <= numero) {
            throw new ValorInvalidoException(
                    "O número da nova versão deve ser maior que o da versão atual.");
        }

        return new VersaoPlano(
                novoNumero,
                EstadoVersao.RASCUNHO,
                copiarTreinos(treinos),
                LocalDateTime.now(),
                null
        );
    }

    /**
     * Corrige diretamente uma versão publicada quando não há execução registrada.
     * A consulta ao histórico de execuções é responsabilidade do serviço; o resultado
     * dessa consulta é informado pelo parâmetro possuiExecucao.
     */
    public void corrigir(List<Treino> treinosCorrigidos, boolean possuiExecucao) {

        if (estado != EstadoVersao.PUBLICADA) {
            throw new TransicaoInvalidaException(
                    "A correção direta só pode ser feita em uma versão publicada.");
        }

        if (possuiExecucao) {
            throw new TransicaoInvalidaException(
                    "Esta versão possui execução registrada. Crie uma nova versão para alterá-la.");
        }

        validarEstrutura(treinosCorrigidos);

        List<Treino> substitutos = copiarTreinos(treinosCorrigidos);

        substitutos.forEach(Treino::bloquearEdicao);

        this.treinos = substitutos;
    }

    /**
     * Verifica a estrutura antes de uma publicação, sem alterar o estado.
     */
    public void validarParaPublicacao() {

        validarEstrutura(treinos);

        if (!estado.podeIrPara(EstadoVersao.PUBLICADA)) {
            throw new TransicaoInvalidaException(
                    "A versão não está em um estado que permita publicação.");
        }
    }

    private void validarEstrutura(List<Treino> lista) {

        Objects.requireNonNull(
                lista, "A lista de treinos é obrigatória.");

        if (lista.isEmpty() || lista.size() > 7) {
            throw new ValorInvalidoException(
                    "Uma versão deve possuir entre 1 e 7 treinos para ser publicada.");
        }

        Set<String> nomes = new HashSet<>();

        for (Treino treino : lista) {

            if (treino == null) {
                throw new ValorInvalidoException(
                        "A versão não pode conter treinos nulos.");
            }

            treino.validarParaPublicacao();

            if (!nomes.add(normalizarNome(treino.getNome()))) {
                throw new ValorInvalidoException(
                        "Não pode haver nomes de treino repetidos dentro da mesma versão.");
            }
        }
    }

    private void verificarNomeUnico(String nome) {

        String normalizado = normalizarNome(nome);

        boolean duplicado = treinos.stream()
                .anyMatch(t -> normalizarNome(t.getNome()).equals(normalizado));

        if (duplicado) {
            throw new ValorInvalidoException(
                    "Já existe um treino com esse nome nesta versão.");
        }
    }

    private void verificarSeRascunho() {

        if (estado != EstadoVersao.RASCUNHO) {
            throw new TransicaoInvalidaException(
                    "Somente versões em RASCUNHO podem ser editadas.");
        }
    }

    private static String normalizarNome(String nome) {

        return nome.trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }

    private static List<Treino> copiarTreinos(List<Treino> origem) {

        Objects.requireNonNull(
                origem, "A lista de treinos é obrigatória.");

        List<Treino> copia = new ArrayList<>();

        for (Treino treino : origem) {
            copia.add(Objects.requireNonNull(
                    treino, "A versão não pode conter treinos nulos.").copiar());
        }

        return copia;
    }

    public int getNumero() {
        return numero;
    }

    public EstadoVersao getEstado() {
        return estado;
    }

    public List<Treino> getTreinos() {
        return List.copyOf(treinos);
    }

    public LocalDateTime getCriadaEm() {
        return criadaEm;
    }

    public LocalDateTime getPublicadaEm() {
        return publicadaEm;
    }
}