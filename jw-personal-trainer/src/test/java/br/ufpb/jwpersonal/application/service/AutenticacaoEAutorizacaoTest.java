package br.ufpb.jwpersonal.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.ufpb.jwpersonal.domain.excecao.AutorizacaoNegadaException;
import br.ufpb.jwpersonal.domain.excecao.ValorInvalidoException;
import br.ufpb.jwpersonal.domain.model.ObjetivoPlano;
import br.ufpb.jwpersonal.domain.model.Perfil;
import br.ufpb.jwpersonal.domain.model.PlanoTreino;
import br.ufpb.jwpersonal.domain.model.Usuario;
import br.ufpb.jwpersonal.repository.UsuarioRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AutenticacaoEAutorizacaoTest {

    /** Hasher de teste: não é seguro, serve apenas para isolar a camada de aplicação. */
    static class HasherFake implements SenhaHasher {
        public String gerarHash(String senha) { return "hash:" + senha; }
        public boolean verificar(String senha, String hash) { return gerarHash(senha).equals(hash); }
    }

    static class RepoFake implements UsuarioRepository {
        final Map<UUID, Usuario> porId = new HashMap<>();
        public void salvar(Usuario u) { porId.put(u.getId(), u); }
        public Optional<Usuario> buscarPorId(UUID id) { return Optional.ofNullable(porId.get(id)); }
        public Optional<Usuario> buscarPorEmail(String email) {
            return porId.values().stream().filter(u -> u.getEmail().equals(email)).findFirst();
        }
    }

    private final SenhaHasher hasher = new HasherFake();
    private final Autorizador autorizador = new Autorizador();
    private RepoFake repo;
    private AutenticacaoService servico;
    private Usuario personal;
    private Usuario aluno;

    @BeforeEach
    void preparar() {
        repo = new RepoFake();
        servico = new AutenticacaoService(repo, hasher);
        personal = new Usuario("Joao", "joao@ex.com", hasher.gerarHash("segredo1"), Perfil.PERSONAL);
        aluno = new Usuario("Ana", "ana@ex.com", hasher.gerarHash("segredo2"), Perfil.ALUNO);
        repo.salvar(personal);
        repo.salvar(aluno);
    }

    // ---- AutenticacaoService ----

    @Test
    void autenticaComCredenciaisValidasENormalizaEmail() {
        SessaoUsuario s = servico.autenticar("  JOAO@Ex.com ", "segredo1");
        assertEquals(personal.getId(), s.usuarioId());
        assertEquals(Perfil.PERSONAL, s.perfil());
        assertTrue(s.ehPersonal());
    }

    @Test
    void senhaIncorretaEEmailInexistenteTemMesmaMensagem() {
        var e1 = assertThrows(AutorizacaoNegadaException.class,
                () -> servico.autenticar("joao@ex.com", "errada"));
        var e2 = assertThrows(AutorizacaoNegadaException.class,
                () -> servico.autenticar("naoexiste@ex.com", "segredo1"));
        assertEquals(e1.getMessage(), e2.getMessage());
    }

    @Test
    void contaInativaNaoAutentica() {
        aluno.inativar();
        assertThrows(AutorizacaoNegadaException.class,
                () -> servico.autenticar("ana@ex.com", "segredo2"));
    }

    @Test
    void contaInativaComSenhaErradaNaoRevelaInatividade() {
        aluno.inativar();
        var e = assertThrows(AutorizacaoNegadaException.class,
                () -> servico.autenticar("ana@ex.com", "errada"));
        assertEquals("E-mail ou senha inválidos.", e.getMessage());
    }

    @Test
    void entradasEmBrancoSaoRejeitadas() {
        assertThrows(ValorInvalidoException.class, () -> servico.autenticar(" ", "x"));
        assertThrows(ValorInvalidoException.class, () -> servico.autenticar(null, "x"));
        assertThrows(ValorInvalidoException.class, () -> servico.autenticar("joao@ex.com", ""));
        assertThrows(ValorInvalidoException.class, () -> servico.autenticar("joao@ex.com", null));
    }

    // ---- SessaoUsuario ----

    @Test
    void sessaoNaoExpoeSenhaEValidaCampos() {
        SessaoUsuario s = SessaoUsuario.de(aluno);
        assertTrue(s.ehAluno());
        assertFalse(s.toString().contains("hash"));
        assertThrows(ValorInvalidoException.class,
                () -> new SessaoUsuario(UUID.randomUUID(), " ", "a@b.com", Perfil.ALUNO));
        assertThrows(NullPointerException.class,
                () -> new SessaoUsuario(null, "A", "a@b.com", Perfil.ALUNO));
    }

    // ---- Autorizador (RN01) ----

    @Test
    void sessaoNulaEhNegada() {
        assertThrows(AutorizacaoNegadaException.class, () -> autorizador.exigirAutenticado(null));
        assertThrows(AutorizacaoNegadaException.class, () -> autorizador.exigirPersonal(null));
    }

    @Test
    void exigirPersonalEExigirAluno() {
        SessaoUsuario sp = SessaoUsuario.de(personal);
        SessaoUsuario sa = SessaoUsuario.de(aluno);

        assertDoesNotThrow(() -> autorizador.exigirPersonal(sp));
        assertDoesNotThrow(() -> autorizador.exigirAluno(sa));
        assertThrows(AutorizacaoNegadaException.class, () -> autorizador.exigirPersonal(sa));
        assertThrows(AutorizacaoNegadaException.class, () -> autorizador.exigirAluno(sp));
    }

    @Test
    void personalSoGerenciaOsProprioPlanos() {
        PlanoTreino plano = new PlanoTreino("Plano A", ObjetivoPlano.values()[0], personal.getId());
        Usuario outro = new Usuario("Maria", "maria@ex.com", "h", Perfil.PERSONAL);

        assertDoesNotThrow(() -> autorizador.exigirPersonalDoPlano(SessaoUsuario.de(personal), plano));
        assertThrows(AutorizacaoNegadaException.class,
                () -> autorizador.exigirPersonalDoPlano(SessaoUsuario.de(outro), plano));
        assertThrows(AutorizacaoNegadaException.class,
                () -> autorizador.exigirPersonalDoPlano(SessaoUsuario.de(aluno), plano));
    }

    @Test
    void alunoSoAcessaOsProprioDados() {
        Usuario outroAluno = new Usuario("Bia", "bia@ex.com", "h", Perfil.ALUNO);

        assertDoesNotThrow(() -> autorizador.exigirProprioAluno(SessaoUsuario.de(aluno), aluno.getId()));
        assertThrows(AutorizacaoNegadaException.class,
                () -> autorizador.exigirProprioAluno(SessaoUsuario.de(outroAluno), aluno.getId()));
        assertThrows(AutorizacaoNegadaException.class,
                () -> autorizador.exigirProprioAluno(SessaoUsuario.de(personal), aluno.getId()));
    }

    @Test
    void personalOuProprioAluno() {
        Usuario outroAluno = new Usuario("Bia", "bia@ex.com", "h", Perfil.ALUNO);

        assertDoesNotThrow(() -> autorizador.exigirPersonalOuProprioAluno(SessaoUsuario.de(personal), aluno.getId()));
        assertDoesNotThrow(() -> autorizador.exigirPersonalOuProprioAluno(SessaoUsuario.de(aluno), aluno.getId()));
        assertThrows(AutorizacaoNegadaException.class,
                () -> autorizador.exigirPersonalOuProprioAluno(SessaoUsuario.de(outroAluno), aluno.getId()));
    }
}
