package ch.obya.pta.common.domain.repository;

import java.util.Collection;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import ch.obya.pta.common.domain.entity.Entity;
import ch.obya.pta.common.domain.vo.Identity;
import ch.obya.pta.common.util.search.FindCriteria;

public interface EntityRepository<E extends Entity<E, I, S>, I extends Identity, S> {

    Uni<E> findOne(I id);

    Multi<E> findByCriteria(Collection<FindCriteria> criteria);

    default Multi<E> findAll() {
        return findByCriteria(FindCriteria.empty());
    }

    Uni<E> save(I id, S state);

    default Uni<E> save(Entity<E, I, S> entity) {
        return save(entity.id(), entity.state());
    }

    Uni<Void> remove(I id);
}

