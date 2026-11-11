package br.ufpb.jwpersonal.domain.model;

/**
 * Estados possíveis de uma versão de plano de treino.
 */
public enum EstadoVersao {
    RASCUNHO,
    PUBLICADA,
    ARQUIVADA;

    /**
     * Informa se a transição para o estado indicado é permitida pela RN02.
     */
    public boolean podeIrPara(EstadoVersao destino) {
        if (destino == null) {
            return false;
        }

        return switch (this) {
            case RASCUNHO -> destino == PUBLICADA;
            case PUBLICADA -> destino == ARQUIVADA;
            case ARQUIVADA -> false;
        };
    }
}