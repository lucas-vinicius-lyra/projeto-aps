package br.ufpb.jwpersonal.domain.excecao;

public class RegraDeNegocioException extends RuntimeException {
    public RegraDeNegocioException(String message) {
        super(message);
    }

    public RegraDeNegocioException(String message, Throwable causa) {
        super(message, causa);
    }
}
