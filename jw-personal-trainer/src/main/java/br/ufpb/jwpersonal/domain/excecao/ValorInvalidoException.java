package br.ufpb.jwpersonal.domain.excecao;

public class ValorInvalidoException extends RegraDeNegocioException {
    public ValorInvalidoException(String message) {
        super(message);
    }

    public ValorInvalidoException(String message, Throwable causa) {
        super(message, causa);
    }
}
