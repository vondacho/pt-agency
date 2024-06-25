package ch.obya.pta.common.domain.repository;

import ch.obya.pta.common.domain.entity.Entity;
import ch.obya.pta.common.domain.vo.Identity;
import ch.obya.pta.common.util.search.AttributeFilter;
import io.smallrye.mutiny.Uni;

import java.util.List;
import java.util.Map;

public interface EntityRepository<E extends Entity<E, I, S>, I extends Identity, S> {

    Uni<E> findOne(I id);

    Uni<List<E>> findByCriteria(Map<String, List<AttributeFilter>> criteria);

    Uni<List<E>> findAll();

    Uni<I> save(I id, S state);

    default Uni<I> save(Entity<E, I, S> entity) {
        return save(entity.id(), entity.state());
    }

    Uni<Void> remove(I id);
}

