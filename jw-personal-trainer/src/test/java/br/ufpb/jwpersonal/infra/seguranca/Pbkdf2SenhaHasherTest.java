
package br.ufpb.jwpersonal.infra.seguranca;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Pbkdf2SenhaHasherTest {

    private final Pbkdf2SenhaHasher hasher =
            new Pbkdf2SenhaHasher();

    @Test
    void geraHashEVerificaSenhaCorretamente() {

        String senha = "MinhaSenha123";

        String hash = hasher.gerarHash(senha);

        assertNotEquals(senha, hash);

        assertTrue(hasher.verificar(senha, hash));
    }

    @Test
    void rejeitaSenhaIncorreta() {

        String hash = hasher.gerarHash("SenhaCorreta123");

        assertFalse(
                hasher.verificar("SenhaErrada123", hash)
        );
    }

    @Test
    void geraHashesDiferentesParaMesmaSenha() {

        String senha = "Senha123";

        String hash1 = hasher.gerarHash(senha);
        String hash2 = hasher.gerarHash(senha);

        // O salt aleatório produz hashes diferentes.
        assertNotEquals(hash1, hash2);

        // Ambos continuam validando a mesma senha.
        assertTrue(hasher.verificar(senha, hash1));
        assertTrue(hasher.verificar(senha, hash2));
    }

    @Test
    void rejeitaHashMalformado() {

        assertFalse(
                hasher.verificar(
                        "Senha123",
                        "hash-invalido"
                )
        );
    }

    @Test
    void rejeitaSenhaVaziaAoGerarHash() {

        assertThrows(
                IllegalArgumentException.class,
                () -> hasher.gerarHash("")
        );
    }
}
