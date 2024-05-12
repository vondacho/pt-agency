package ch.obya.pta.common.domain.util;

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

import io.smallrye.mutiny.Uni;

import java.time.Duration;
import java.util.function.Function;

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
