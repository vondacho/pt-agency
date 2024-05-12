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

import ch.obya.pta.common.domain.event.*;
import ch.obya.pta.common.domain.vo.Identity;
import ch.obya.pta.common.domain.util.CommonProblem;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

import static ch.obya.pta.common.domain.util.CommonProblem.ifNullThrow;

@ToString(of = {"id", "state"}, doNotUseGetters = true)
@EqualsAndHashCode(of = "id", callSuper = false)
@Accessors(chain = true, fluent = true)
public abstract class BaseEntity<E extends BaseEntity<E, I, S>, I extends Identity, S> implements Entity<E, I, S> {
    @Getter
    protected I id;
    protected S state;
    private final List<Event> events = new ArrayList<>();

    protected BaseEntity(I id, S state) {
        ifNullThrow(id, CommonProblem.AttributeNotNull.toException("Entity.id"));
        ifNullThrow(state, CommonProblem.AttributeNotNull.toException("Entity.state"));
        this.id = id;
        this.state = state;
    }

    @Override
    public S state() {
        return cloneState();
    }

    @Override
    public List<Event> domainEvents() {
        var result = List.copyOf(events);
        events.clear();
        return result;
    }

    protected S validate(S state) {
        return state;
    }

    protected E andEvent(Event...e) {
        events.addAll(List.of(e));
        return (E) this;
    }

    protected abstract S cloneState();
}
