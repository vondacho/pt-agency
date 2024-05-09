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

import ch.obya.pta.common.domain.entity.BaseEntity;
import ch.obya.pta.common.domain.entity.Entity;
import ch.obya.pta.common.domain.vo.Identity;
import ch.obya.pta.common.util.search.FindCriteria;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
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
    public Optional<E> findOne(I id) {
        return Optional.ofNullable(store.get(id)).map(s -> entityCreator.apply(id, s));
    }

    @Override
    public List<E> findByCriteria(List<FindCriteria> criteria) {
        return store.entrySet().stream().map(e -> entityCreator.apply(e.getKey(), e.getValue())).collect(toList());
    }

    @Override
    public E save(I id, S state) {
        store.put(id, state);
        return entityCreator.apply(id, state);
    }

    @Override
    public void delete(I id) {
        store.remove(id);
    }

    private class DefaultEntity extends BaseEntity<DefaultEntity, I, S> {

        DefaultEntity(I identity, S state) {
            super(identity, state);
        }

        @Override
        public S validate(S state) {
            throw new UnsupportedOperationException();
        }
    }
}
