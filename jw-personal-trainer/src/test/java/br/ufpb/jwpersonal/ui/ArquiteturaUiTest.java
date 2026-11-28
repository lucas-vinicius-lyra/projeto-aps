package br.ufpb.jwpersonal.ui;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ArquiteturaUiTest {

    private static final Pattern PROIBIDO = Pattern.compile(
            "br\\.ufpb\\.jwpersonal\\.(repository|infra)\\."
                    + "|application\\.service\\.(\\*|\\w*Service\\b|Autorizador)"
                    + "|JOptionPane");

    @Test
    void interfaceUsaSomenteAFacade() throws IOException {
        Path pasta = Path.of("src/main/java/br/ufpb/jwpersonal/ui");

        try (Stream<Path> arquivos = Files.list(pasta)) {
            for (Path arquivo : arquivos.toList()) {
                String codigo = Files.readString(arquivo);

                // O comentário do pacote cita os nomes; só o código importa.
                if (!arquivo.endsWith("package-info.java")) {
                    assertFalse(PROIBIDO.matcher(codigo).find(), arquivo.toString());
                }
            }
        }
    }
}
