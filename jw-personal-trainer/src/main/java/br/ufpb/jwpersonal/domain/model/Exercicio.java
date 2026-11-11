package br.ufpb.jwpersonal.domain.model;

import br.ufpb.jwpersonal.domain.excecao.ValorInvalidoException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.UUID;

/**
 * Exercício disponível no catálogo, com conteúdo de apoio para o aluno.
 * A prescrição guarda o ID do exercício; instruções e referências podem ser atualizadas.
 */
public final class Exercicio {

    private final UUID id;
    private final String nome;
    private String instrucoes;
    private String videoUrl;
    private String fonteReferencia;
    private boolean ativo;

    public Exercicio(String nome, String instrucoes, String videoUrl,
                     String fonteReferencia) {

        this(UUID.randomUUID(), nome, instrucoes, videoUrl,
                fonteReferencia, true);
    }

    public Exercicio(UUID id, String nome, String instrucoes, String videoUrl,
                     String fonteReferencia, boolean ativo) {

        this.id = Objects.requireNonNull(
                id, "O ID do exercício é obrigatório.");

        if (nome == null || nome.isBlank()) {
            throw new ValorInvalidoException(
                    "O nome do exercício é obrigatório.");
        }

        this.nome = nome.trim();
        this.instrucoes = normalizarOpcional(instrucoes);
        this.videoUrl = validarUrlOpcional(videoUrl);
        this.fonteReferencia = normalizarOpcional(fonteReferencia);
        this.ativo = ativo;
    }

    /**
     * Atualiza as instruções e a fonte de referência do exercício.
     */
    public void atualizarInstrucoes(String novasInstrucoes,
                                    String novaFonteReferencia) {

        if (novasInstrucoes == null || novasInstrucoes.isBlank()) {
            throw new ValorInvalidoException(
                    "As instruções do exercício não podem ficar vazias.");
        }

        this.instrucoes = novasInstrucoes.trim();
        this.fonteReferencia = normalizarOpcional(novaFonteReferencia);
    }

    /**
     * Atualiza todo o conteúdo de apoio, sem alterar prescrições já registradas.
     */
    public void atualizarConteudoApoio(String novasInstrucoes,
                                       String novoVideoUrl,
                                       String novaFonteReferencia) {

        // Valida tudo antes de alterar o objeto, evitando atualização parcial.
        String urlValidada = validarUrlOpcional(novoVideoUrl);

        atualizarInstrucoes(novasInstrucoes, novaFonteReferencia);

        this.videoUrl = urlValidada;
    }

    /**
     * Inativa o exercício. Não existe exclusão física nesta entidade.
     */
    public void inativar() {
        ativo = false;
    }

    private static String normalizarOpcional(String valor) {

        return valor == null || valor.isBlank()
                ? null
                : valor.trim();
    }

    private static String validarUrlOpcional(String valor) {

        String url = normalizarOpcional(valor);

        if (url == null) {
            return null;
        }

        try {

            URI uri = new URI(url);

            String esquema = uri.getScheme();

            if (uri.getHost() == null || esquema == null
                    || !(esquema.equalsIgnoreCase("http")
                    || esquema.equalsIgnoreCase("https"))) {

                throw new ValorInvalidoException(
                        "O link do vídeo deve ser uma URL válida iniciada por http ou https.");
            }

            return uri.toString();

        } catch (URISyntaxException e) {

            throw new ValorInvalidoException(
                    "O link do vídeo informado é inválido.", e);
        }
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getInstrucoes() {
        return instrucoes;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public String getFonteReferencia() {
        return fonteReferencia;
    }

    public boolean isAtivo() {
        return ativo;
    }
}