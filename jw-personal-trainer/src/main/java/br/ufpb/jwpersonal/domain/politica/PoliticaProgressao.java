package br.ufpb.jwpersonal.domain.politica;

import br.ufpb.jwpersonal.domain.model.ItemTreino;

import java.util.function.Function;

/**
 * Contrato do padrão Strategy para as políticas de progressão dos treinos.
 *
 * Cada implementação define como um ItemTreino deve ser ajustado.
 */
@FunctionalInterface
public interface PoliticaProgressao
        extends Function<ItemTreino, ItemTreino> {

    /**
     * Calcula e devolve um novo item de treino,
     * sem alterar o item original.
     */
    ItemTreino progredir(ItemTreino item);

    /**
     * Adapta o contrato Strategy ao Function utilizado
     * pelo método aplicarProgressao da VersaoPlano.
     */
    @Override
    default ItemTreino apply(ItemTreino item) {
        return progredir(item);
    }
}