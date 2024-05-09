package ch.obya.pta.booking.domain.entity;

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

import ch.obya.pta.booking.domain.vo.BookingId;
import ch.obya.pta.booking.domain.vo.ParticipantId;
import ch.obya.pta.booking.domain.vo.SessionId;
import ch.obya.pta.booking.domain.vo.SubscriptionId;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;

@Accessors(fluent = true)
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Booking {
    BookingId id;
    SubscriptionId subscription;
    Status status;

    public enum Status {
        DONE, WAITING_LIST, PREBOOKED
    }

    public static Booking done(SessionId session, ParticipantId participant, SubscriptionId subscription) {
        return new Booking(new BookingId(session, participant), subscription, Status.DONE);
    }

    public Booking confirmed() {
        return done(id.session(), id.participant(), subscription);
    }

    public static Booking waiting(SessionId session, ParticipantId participant, SubscriptionId subscription) {
        return new Booking(new BookingId(session, participant), subscription, Status.WAITING_LIST);
    }

    public static Booking prebooked(SessionId session, ParticipantId participant, SubscriptionId subscription) {
        return new Booking(new BookingId(session, participant), subscription, Status.PREBOOKED);
    }
}
