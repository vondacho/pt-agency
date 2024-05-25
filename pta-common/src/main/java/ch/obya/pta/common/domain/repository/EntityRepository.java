package ch.obya.pta.common.domain.repository;

/*-
 * #%L
 * pta-common
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2024 obya
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import ch.obya.pta.common.domain.entity.Entity;
import ch.obya.pta.common.domain.vo.Identity;
import ch.obya.pta.common.util.search.FindCriteria;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import java.util.Collection;

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

