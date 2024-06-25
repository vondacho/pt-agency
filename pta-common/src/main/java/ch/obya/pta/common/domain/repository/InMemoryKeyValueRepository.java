package ch.obya.pta.common.domain.repository;

import ch.obya.pta.common.domain.entity.BaseEntity;
import ch.obya.pta.common.domain.entity.Entity;
import ch.obya.pta.common.domain.vo.Identity;
import ch.obya.pta.common.util.search.AttributeFilter;
import io.smallrye.mutiny.Uni;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import static java.util.stream.Collectors.toList;

@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PROTECTED)
public class InMemoryKeyValueRepository<E extends Entity<E, I, S>, I extends Identity, S> implements EntityRepository<E, I, S> {

    HashMap<I, S> store = new HashMap<>();

    BiFunction<I, S, E> entityCreator = (id, state) -> (E) new DefaultEntity(id, state);

    public InMemoryKeyValueRepository(@NonNull BiFunction<I, S, E> entityCreator) {
        this.entityCreator = entityCreator;
    }

    @Override
    public Uni<E> findOne(I id) {
        return Uni.createFrom().item(store.get(id)).map(s -> entityCreator.apply(id, s));
    }

    @Override
    public Uni<List<E>> findByCriteria(Map<String, List<AttributeFilter>> criteria) {
        return findAll();
    }

    @Override
    public Uni<List<E>> findAll() {
        return Uni.createFrom().item(store.entrySet().stream().map(e -> entityCreator.apply(e.getKey(), e.getValue())).collect(toList()));
    }

    @Override
    public Uni<I> save(I id, S state) {
        store.put(id, state);
        return Uni.createFrom().item(entityCreator.apply(id, state)).map(Entity::id);
    }

    @Override
    public Uni<Void> remove(I id) {
        store.remove(id);
        return Uni.createFrom().voidItem();
    }

    private class DefaultEntity extends BaseEntity<DefaultEntity, I, S> {
        DefaultEntity(I identity, S state) {
            super(identity, state);
        }

        @Override
        protected S cloneState() {
            return state;
        }
    }
}
