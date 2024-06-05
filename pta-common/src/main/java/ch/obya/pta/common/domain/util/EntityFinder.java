package ch.obya.pta.common.domain.util;

import java.time.Duration;
import java.util.function.Function;

import io.smallrye.mutiny.Uni;

public class EntityFinder {

    private EntityFinder() {
    }

    public static <E,I> Uni<E> find(Class<E> clazz, I id, Function<I, Uni<E>> finder, Problem doesNotExist) {
        return finder
                .apply(id)
                .ifNoItem().after(Duration.ofMillis(100))
                .failWith(doesNotExist.toException(clazz.getSimpleName(), id.toString()));
    }
}
