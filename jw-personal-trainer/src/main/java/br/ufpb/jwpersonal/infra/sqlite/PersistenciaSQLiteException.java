
package br.ufpb.jwpersonal.infra.sqlite;

/**
 * Representa uma falha durante uma operação de persistência SQLite.
 */
public class PersistenciaSQLiteException extends RuntimeException {

    public PersistenciaSQLiteException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
