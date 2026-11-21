package br.ufpb.jwpersonal.application.service;

import br.ufpb.jwpersonal.domain.excecao.AutorizacaoNegadaException;
import br.ufpb.jwpersonal.domain.excecao.ValorInvalidoException;
import br.ufpb.jwpersonal.domain.model.Usuario;
import br.ufpb.jwpersonal.repository.UsuarioRepository;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Autentica usuários por e-mail e senha e cria a {@link SessaoUsuario}.
 */
public class AutenticacaoService {

    // Mensagem única para e-mail inexistente e senha incorreta, evitando revelar quais e-mails existem.
    private static final String CREDENCIAIS_INVALIDAS = "E-mail ou senha inválidos.";

    private final UsuarioRepository usuarioRepository;
    private final SenhaHasher senhaHasher;

    public AutenticacaoService(UsuarioRepository usuarioRepository, SenhaHasher senhaHasher) {
        this.usuarioRepository = Objects.requireNonNull(
                usuarioRepository, "O repositório de usuários é obrigatório.");
        this.senhaHasher = Objects.requireNonNull(
                senhaHasher, "O gerador de hash de senha é obrigatório.");
    }

    /**
     * Autentica o usuário e devolve a sessão correspondente.
     *
     * @throws ValorInvalidoException      se e-mail ou senha estiverem em branco
     * @throws AutorizacaoNegadaException  se as credenciais forem inválidas ou a conta estiver inativa
     */
    public SessaoUsuario autenticar(String email, String senha) {

        if (email == null || email.isBlank()) {
            throw new ValorInvalidoException("O e-mail é obrigatório.");
        }

        if (senha == null || senha.isEmpty()) {
            throw new ValorInvalidoException("A senha é obrigatória.");
        }

        String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);

        Optional<Usuario> encontrado = usuarioRepository.buscarPorEmail(emailNormalizado);

        if (encontrado.isEmpty()) {
            throw new AutorizacaoNegadaException(CREDENCIAIS_INVALIDAS);
        }

        Usuario usuario = encontrado.get();

        if (!senhaHasher.verificar(senha, usuario.getSenhaHash())) {
            throw new AutorizacaoNegadaException(CREDENCIAIS_INVALIDAS);
        }

        // A conta só é reportada como inativa depois de a senha ser confirmada.
        if (!usuario.isAtivo()) {
            throw new AutorizacaoNegadaException("A conta do usuário está inativa.");
        }

        return SessaoUsuario.de(usuario);
    }
}
