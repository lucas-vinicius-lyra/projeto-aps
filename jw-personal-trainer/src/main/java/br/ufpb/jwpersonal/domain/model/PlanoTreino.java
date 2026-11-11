package br.ufpb.jwpersonal.domain.model;

import br.ufpb.jwpersonal.domain.excecao.RecursoNaoEncontradoException;
import br.ufpb.jwpersonal.domain.excecao.TransicaoInvalidaException;
import br.ufpb.jwpersonal.domain.excecao.ValorInvalidoException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Raiz do agregado de plano de treino. Mantém os dados gerais e o histórico
 * de versões; ao ser criado, já inicia a versão 1 em RASCUNHO (CU01).
 */
public final class PlanoTreino {

    private final UUID id;
    private final String titulo;
    private final ObjetivoPlano objetivo;
    private final UUID personalId;
    private final List<VersaoPlano> versoes;

    public PlanoTreino(String titulo, ObjetivoPlano objetivo, UUID personalId) {

        this(UUID.randomUUID(), titulo, objetivo, personalId);
    }

    public PlanoTreino(UUID id, String titulo,
                       ObjetivoPlano objetivo, UUID personalId) {

        this.id = Objects.requireNonNull(
                id, "O ID do plano é obrigatório.");

        if (titulo == null || titulo.isBlank()) {
            throw new ValorInvalidoException(
                    "O título do plano é obrigatório.");
        }

        this.titulo = titulo.trim();

        this.objetivo = Objects.requireNonNull(
                objetivo, "O objetivo do plano é obrigatório.");

        this.personalId = Objects.requireNonNull(
                personalId, "O personal responsável é obrigatório.");

        this.versoes = new ArrayList<>();

        // Todo plano começa com a versão 1 em RASCUNHO.
        this.versoes.add(new VersaoPlano(1));
    }

    /**
     * Construtor para reconstruir um plano carregado da persistência.
     * Diferentemente do construtor de criação, recebe o histórico já existente.
     */
    public PlanoTreino(UUID id, String titulo, ObjetivoPlano objetivo,
                       UUID personalId, List<VersaoPlano> historicoExistente) {

        this.id = Objects.requireNonNull(
                id, "O ID do plano é obrigatório.");

        if (titulo == null || titulo.isBlank()) {
            throw new ValorInvalidoException(
                    "O título do plano é obrigatório.");
        }

        this.titulo = titulo.trim();

        this.objetivo = Objects.requireNonNull(
                objetivo, "O objetivo do plano é obrigatório.");

        this.personalId = Objects.requireNonNull(
                personalId, "O personal responsável é obrigatório.");

        Objects.requireNonNull(
                historicoExistente, "O histórico de versões é obrigatório.");

        if (historicoExistente.isEmpty()) {
            throw new ValorInvalidoException(
                    "Um plano persistido deve possuir ao menos uma versão.");
        }

        if (historicoExistente.stream().anyMatch(Objects::isNull)) {
            throw new ValorInvalidoException(
                    "O histórico não pode conter versões nulas.");
        }

        this.versoes = new ArrayList<>(historicoExistente);

        validarHistorico();
    }

    private void validarHistorico() {

        long rascunhos = versoes.stream()
                .filter(v -> v.getEstado() == EstadoVersao.RASCUNHO)
                .count();

        long publicadas = versoes.stream()
                .filter(v -> v.getEstado() == EstadoVersao.PUBLICADA)
                .count();

        long numerosDistintos = versoes.stream()
                .map(VersaoPlano::getNumero)
                .distinct()
                .count();

        if (numerosDistintos != versoes.size()
                || rascunhos > 1
                || publicadas > 1) {

            throw new ValorInvalidoException(
                    "O histórico contém números repetidos ou mais de um rascunho/versão publicada.");
        }
    }

    /**
     * Inicia a revisão copiando a última versão publicada para um novo rascunho.
     */
    public VersaoPlano iniciarRevisao() {

        if (rascunhoAberto().isPresent()) {
            throw new TransicaoInvalidaException(
                    "Já existe um rascunho aberto para este plano.");
        }

        VersaoPlano atual = versaoPublicada().orElseThrow(() ->
                new TransicaoInvalidaException(
                        "É necessário possuir uma versão publicada para iniciar uma revisão."));

        int proximoNumero = versoes.stream()
                .mapToInt(VersaoPlano::getNumero)
                .max()
                .orElse(0) + 1;

        VersaoPlano novaVersao =
                atual.copiarComoRascunho(proximoNumero);

        versoes.add(novaVersao);

        return novaVersao;
    }

    /**
     * Publica o rascunho indicado e arquiva a versão publicada anteriormente.
     * O serviço deverá persistir essa operação dentro de uma UnidadeDeTrabalho.
     */
    public void publicar(int numero) {

        VersaoPlano novaVersao = versaoPorNumero(numero);

        if (novaVersao.getEstado() != EstadoVersao.RASCUNHO) {
            throw new TransicaoInvalidaException(
                    "Somente uma versão em RASCUNHO pode ser publicada.");
        }

        Optional<VersaoPlano> anterior = versaoPublicada();

        novaVersao.validarParaPublicacao();

        if (anterior.isPresent()
                && !anterior.get().getEstado()
                .podeIrPara(EstadoVersao.ARQUIVADA)) {

            throw new TransicaoInvalidaException(
                    "A versão publicada anteriormente não pode ser arquivada.");
        }

        // As validações são feitas antes das mudanças para evitar estado parcial no agregado.
        novaVersao.publicar();

        anterior.ifPresent(VersaoPlano::arquivar);
    }

    /**
     * Retorna a versão atualmente publicada.
     */
    public Optional<VersaoPlano> versaoPublicada() {

        return versoes.stream()
                .filter(v -> v.getEstado() == EstadoVersao.PUBLICADA)
                .max(Comparator.comparingInt(VersaoPlano::getNumero));
    }

    /**
     * Retorna o rascunho aberto, caso exista.
     */
    public Optional<VersaoPlano> rascunhoAberto() {

        return versoes.stream()
                .filter(v -> v.getEstado() == EstadoVersao.RASCUNHO)
                .max(Comparator.comparingInt(VersaoPlano::getNumero));
    }

    /**
     * Retorna o histórico de versões em ordem crescente.
     */
    public List<VersaoPlano> historico() {

        return versoes.stream()
                .sorted(Comparator.comparingInt(VersaoPlano::getNumero))
                .toList();
    }

    /**
     * Localiza uma versão pelo número.
     */
    public VersaoPlano versaoPorNumero(int numero) {

        return versoes.stream()
                .filter(v -> v.getNumero() == numero)
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "A versão " + numero + " não foi encontrada neste plano."));
    }

    /**
     * Inclui uma versão recuperada da persistência, sem duplicar seu número.
     */
    public void restaurarVersao(VersaoPlano versao) {

        Objects.requireNonNull(versao, "A versão é obrigatória.");

        boolean jaExiste = versoes.stream()
                .anyMatch(v -> v.getNumero() == versao.getNumero());

        if (jaExiste) {
            throw new ValorInvalidoException(
                    "Já existe essa versão no histórico do plano.");
        }

        versoes.add(versao);

        try {
            validarHistorico();
        } catch (RuntimeException erro) {
            versoes.remove(versao);
            throw erro;
        }
    }

    public UUID getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public ObjetivoPlano getObjetivo() {
        return objetivo;
    }

    public UUID getPersonalId() {
        return personalId;
    }

    public List<VersaoPlano> getVersoes() {
        return historico();
    }
}