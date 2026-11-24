
package br.ufpb.jwpersonal.infra.seguranca;

import br.ufpb.jwpersonal.application.service.SenhaHasher;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Implementação de hash de senhas utilizando PBKDF2.
 *
 * Utiliza:
 * - PBKDF2WithHmacSHA256
 * - Salt aleatório
 * - 600.000 iterações
 * - Chave derivada de 256 bits
 */
public class Pbkdf2SenhaHasher implements SenhaHasher {

    private static final String ALGORITMO =
            "PBKDF2WithHmacSHA256";

    private static final String PREFIXO =
            "pbkdf2-sha256";

    private static final int ITERACOES = 600_000;

    private static final int TAMANHO_SALT = 16;

    private static final int TAMANHO_CHAVE = 256;

    private static final int ITERACOES_MINIMAS = 100_000;

    private static final int ITERACOES_MAXIMAS = 1_000_000;

    private final SecureRandom random = new SecureRandom();

    /**
     * Gera um hash seguro para a senha informada.
     */
    @Override
    public String gerarHash(String senha) {

        if (senha == null || senha.isBlank()) {
            throw new IllegalArgumentException(
                    "A senha não pode ser nula ou vazia."
            );
        }

        byte[] salt = new byte[TAMANHO_SALT];

        random.nextBytes(salt);

        byte[] hash = derivarChave(
                senha.toCharArray(),
                salt,
                ITERACOES
        );

        String saltCodificado =
                Base64.getEncoder().encodeToString(salt);

        String hashCodificado =
                Base64.getEncoder().encodeToString(hash);

        return PREFIXO + "$"
                + ITERACOES + "$"
                + saltCodificado + "$"
                + hashCodificado;
    }

    /**
     * Verifica se a senha corresponde ao hash armazenado.
     */
    @Override
    public boolean verificar(
            String senha,
            String hashArmazenado) {

        if (senha == null || senha.isEmpty()
                || hashArmazenado == null) {
            return false;
        }

        String[] partes = hashArmazenado.split("\\$", -1);

        if (partes.length != 4
                || !PREFIXO.equals(partes[0])) {
            return false;
        }

        int iteracoes;
        byte[] salt;
        byte[] hashOriginal;

        try {
            iteracoes = Integer.parseInt(partes[1]);

            salt = Base64.getDecoder().decode(partes[2]);

            hashOriginal = Base64.getDecoder().decode(partes[3]);

        } catch (IllegalArgumentException e) {
            return false;
        }

        if (iteracoes < ITERACOES_MINIMAS
                || iteracoes > ITERACOES_MAXIMAS
                || salt.length != TAMANHO_SALT
                || hashOriginal.length != TAMANHO_CHAVE / 8) {
            return false;
        }

        byte[] hashCalculado = derivarChave(
                senha.toCharArray(),
                salt,
                iteracoes
        );

        // Comparação apropriada para evitar comparação
        // simples de bytes com possível vazamento temporal.
        return MessageDigest.isEqual(
                hashOriginal,
                hashCalculado
        );
    }

    /**
     * Deriva uma chave utilizando PBKDF2.
     */
    private byte[] derivarChave(
            char[] senha,
            byte[] salt,
            int iteracoes) {

        PBEKeySpec especificacao = new PBEKeySpec(
                senha,
                salt,
                iteracoes,
                TAMANHO_CHAVE
        );

        try {
            SecretKeyFactory fabrica =
                    SecretKeyFactory.getInstance(ALGORITMO);

            return fabrica
                    .generateSecret(especificacao)
                    .getEncoded();

        } catch (GeneralSecurityException e) {

            throw new IllegalStateException(
                    "Não foi possível gerar o hash da senha.",
                    e
            );

        } finally {

            especificacao.clearPassword();
            Arrays.fill(senha, '\0');
        }
    }
}
