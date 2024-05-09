package ch.obya.pta.booking.domain.vo;

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

import ch.obya.pta.common.util.exception.CommonProblem;

import static ch.obya.pta.common.util.exception.CommonProblem.ifNullThrow;

public record BookingId(SessionId session, ParticipantId participant) {
    public BookingId {
        ifNullThrow(session, CommonProblem.AttributeNotNull.toException("BookingId.session"));
        ifNullThrow(participant, CommonProblem.AttributeNotNull.toException("BookingId.participant"));
    }

    public boolean equals(SessionId session, ParticipantId participant) {
        return this.session.equals(session) && this.participant.equals(participant);
    }
}