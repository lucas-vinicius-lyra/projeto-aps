package br.ufpb.jwpersonal.application.service;

import br.ufpb.jwpersonal.domain.excecao.AutorizacaoNegadaException;
import br.ufpb.jwpersonal.domain.model.PlanoTreino;

import java.util.Objects;
import java.util.UUID;

/**
 * Centraliza as verificações de permissão (RN01).
 *
 * <ul>
 *   <li>PERSONAL gerencia exercícios, planos e atribuições, e somente os planos
 *       sob a sua responsabilidade.</li>
 *   <li>ALUNO acessa apenas os próprios dados e registra as próprias execuções.</li>
 * </ul>
 *
 * Todos os métodos lançam {@link AutorizacaoNegadaException} quando a operação
 * não é permitida. Os serviços devem chamá-los antes de qualquer leitura ou gravação.
 */
public class Autorizador {

    /**
     * Exige uma sessão autenticada.
     */
    public void exigirAutenticado(SessaoUsuario sessao) {
        if (sessao == null) {
            throw new AutorizacaoNegadaException(
                    "É necessário estar autenticado para realizar esta operação.");
        }
    }

    /**
     * Exige perfil PERSONAL.
     */
    public void exigirPersonal(SessaoUsuario sessao) {
        exigirAutenticado(sessao);

        if (!sessao.ehPersonal()) {
            throw new AutorizacaoNegadaException(
                    "Operação permitida apenas para o perfil PERSONAL.");
        }
    }

    /**
     * Exige perfil ALUNO.
     */
    public void exigirAluno(SessaoUsuario sessao) {
        exigirAutenticado(sessao);

        if (!sessao.ehAluno()) {
            throw new AutorizacaoNegadaException(
                    "Operação permitida apenas para o perfil ALUNO.");
        }
    }

    /**
     * Exige que a sessão seja do personal responsável pelo plano.
     */
    public void exigirPersonalDoPlano(SessaoUsuario sessao, PlanoTreino plano) {
        exigirPersonal(sessao);
        Objects.requireNonNull(plano, "O plano é obrigatório.");

        if (!sessao.pertenceA(plano.getPersonalId())) {
            throw new AutorizacaoNegadaException(
                    "O plano pertence a outro personal.");
        }
    }

    /**
     * Exige que a sessão seja do próprio aluno indicado.
     */
    public void exigirProprioAluno(SessaoUsuario sessao, UUID alunoId) {
        exigirAluno(sessao);
        Objects.requireNonNull(alunoId, "O aluno é obrigatório.");

        if (!sessao.pertenceA(alunoId)) {
            throw new AutorizacaoNegadaException(
                    "O aluno só pode acessar os próprios dados.");
        }
    }

    /**
     * Permite o acesso a um personal ou ao próprio aluno.
     * Usado em consultas, como histórico de execuções e atribuições.
     */
    public void exigirPersonalOuProprioAluno(SessaoUsuario sessao, UUID alunoId) {
        exigirAutenticado(sessao);
        Objects.requireNonNull(alunoId, "O aluno é obrigatório.");

        if (sessao.ehPersonal()) {
            return;
        }

        if (!sessao.pertenceA(alunoId)) {
            throw new AutorizacaoNegadaException(
                    "O aluno só pode acessar os próprios dados.");
        }
    }
}
