package ch.obya.pta.common.domain.entity;

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

import ch.obya.pta.common.domain.event.Event;
import ch.obya.pta.common.domain.vo.Identity;

import java.util.List;

/**
 * This interface specifies the minimal api of a domain entity, basically composed of an identity and a state.
 *
 * @param <E> Any class that implements {@link Entity}
 * @param <I> Any class that implements {@link Identity}
 * @param <S> Any class
 */
public interface Entity<E extends Entity<E, I, S>, I extends Identity, S> {

    /**
     * @return the identity of the entity
     */
    I id();

    /**
     * @return a snapshot of the current state hosted by the entity
     */
    S state();

    /**
     * @return the list of domain {@link Event} instances emitted and accumulated when invoking the entity methods.
     * A second call returns an empty result.
     */
    List<Event> domainEvents();
}
