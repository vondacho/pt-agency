package ch.obya.pta.common.domain.util;

import java.util.function.Function;

import io.smallrye.mutiny.Uni;

public class EntityFinder {

    private EntityFinder() {
    }

    /**
     * Looks an entity up and fails with {@code doesNotExist} when the finder yields no entity, i.e. a {@code null} item,
     * which is how repositories report an unknown id. Failures of the finder itself are propagated unchanged.
     */
    public static <E, I> Uni<E> find(Class<E> clazz, I id, Function<I, Uni<E>> finder, Problem doesNotExist) {
        return finder
                .apply(id)
                .onItem().ifNull()
                .failWith(doesNotExist.toException(clazz.getSimpleName(), id.toString()));
    }
}
