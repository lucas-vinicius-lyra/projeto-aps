package br.ufpb.jwpersonal.domain.model;

public enum EstadoVersao {
    RASCUNHO,
    PUBLICADA,
    ARQUIVADA;

    public boolean podeIrPara(EstadoVersao versaoDestino){
        return switch (this){
            case RASCUNHO -> versaoDestino == PUBLICADA;
            case PUBLICADA -> versaoDestino == ARQUIVADA;
            case ARQUIVADA -> false;
        };
    }
}
