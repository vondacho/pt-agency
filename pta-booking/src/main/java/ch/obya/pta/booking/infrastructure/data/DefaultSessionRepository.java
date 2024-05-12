package ch.obya.pta.booking.infrastructure.data;

/*-
 * #%L
 * pta-booking
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

import ch.obya.pta.booking.domain.aggregate.Session;
import ch.obya.pta.booking.domain.repository.SessionRepository;
import ch.obya.pta.booking.domain.vo.SessionId;
import ch.obya.pta.common.util.search.FindCriteria;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Collection;
import java.util.List;

@ApplicationScoped
public class DefaultSessionRepository implements SessionRepository {

    @Override
    public Uni<Session> findOne(SessionId id) {
        return null;
    }

    @Override
    public Uni<List<Session>> findByCriteria(Collection<FindCriteria> criteria) {
        return null;
    }

    @Override
    public Uni<Session> save(SessionId id, Session.State state) {
        return null;
    }

    @Override
    public Uni<Void> remove(SessionId id) {
        return null;
    }
}
