package ch.obya.pta.common.infrastructure.messaging;

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

import ch.obya.pta.common.application.EventPublisher;
import ch.obya.pta.common.domain.event.Event;
import io.smallrye.mutiny.Uni;

import java.util.Collection;

public class RdbmsEventPublisher implements EventPublisher {
    @Override
    public Uni<Void> publish(Collection<Event> events) {
        return null;
    }
}
