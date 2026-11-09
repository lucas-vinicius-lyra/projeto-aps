package br.ufpb.jwpersonal.domain.excecao;

public class RecursoNaoEncontradoException extends RegraDeNegocioException {
    public RecursoNaoEncontradoException(String message) {
        super(message);
    }

    public RecursoNaoEncontradoException(String message, Throwable causa) {
        super(message, causa);
    }
}
