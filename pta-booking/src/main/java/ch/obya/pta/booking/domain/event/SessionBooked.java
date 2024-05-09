package ch.obya.pta.booking.domain.event;

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

import ch.obya.pta.booking.domain.vo.ParticipantId;
import ch.obya.pta.booking.domain.vo.SessionId;
import ch.obya.pta.common.domain.event.Event;
import ch.obya.pta.common.util.exception.CommonProblem;

import java.time.Instant;

import static ch.obya.pta.common.util.exception.CommonProblem.ifNullThrow;
import static java.util.Optional.ofNullable;

public record SessionBooked(SessionId session, ParticipantId participant, Instant timestamp) implements Event {
    public SessionBooked {
        ifNullThrow(session, CommonProblem.AttributeNotNull.toException("SessionBooked.session"));
        ifNullThrow(participant, CommonProblem.AttributeNotNull.toException("SessionBooked.participant"));
        timestamp = ofNullable(timestamp).orElseGet(Instant::now);
    }

    public SessionBooked(SessionId session, ParticipantId participant) {
        this(session, participant, null);
    }
}
