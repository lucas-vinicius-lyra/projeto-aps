package br.ufpb.jwpersonal.domain.excecao;

public class TransicaoInvalidaException extends RegraDeNegocioException {
    public TransicaoInvalidaException(String message) {
        super(message);
    }

    public TransicaoInvalidaException(String message, Throwable causa) {
        super(message, causa);
    }
}
