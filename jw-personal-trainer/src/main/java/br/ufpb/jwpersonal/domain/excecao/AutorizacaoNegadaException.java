package br.ufpb.jwpersonal.domain.excecao;

public class AutorizacaoNegadaException extends RegraDeNegocioException {
    public AutorizacaoNegadaException(String message) {
        super(message);
    }

    public AutorizacaoNegadaException(String message, Throwable causa) {
        super(message, causa);
    }
}
