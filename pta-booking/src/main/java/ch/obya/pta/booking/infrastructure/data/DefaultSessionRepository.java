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

import ch.obya.pta.booking.domain.*;
import io.quarkus.arc.Unremovable;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Unremovable
@ApplicationScoped
public class DefaultSessionRepository implements SessionRepository {
    @Override
    public Uni<Session> findById(SessionId sessionId) {
        return Uni.createFrom().item(sessionMock(sessionId));
    }

    @Override
    public Uni<List<Session>> findAllFromTo(LocalDate from, LocalDate to) {
        return Uni.createFrom().item(List.of(sessionMock()));
    }

    @Override
    public Uni<Session> findByBookingId(BookingId booking) {
        return Uni.createFrom().item(sessionMock(booking));
    }

    @Override
    public Uni<Session> persist(Session session) {
        return Uni.createFrom().item(session);
    }

    private Session sessionMock(SessionId id) {
        return new Session(
                id,
                new ArticleId(UUID.randomUUID()),
                "test",
                new Session.TimeSlot(LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(10, 0), Duration.ofHours(1)),
                new Location("one room"),
                new Quota(3, 25),
                new HashSet<>());
    }

    private Session sessionMock(BookingId id) {
        var sessionId = new SessionId(UUID.randomUUID());
        return new Session(
                new SessionId(UUID.randomUUID()),
                new ArticleId(UUID.randomUUID()),
                "test",
                new Session.TimeSlot(LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(10, 0), Duration.ofHours(1)),
                new Location("one room"),
                new Quota(3, 25),
                new HashSet<>(Set.of(prebookingMock(sessionId, id))));
    }

    private Booking prebookingMock(SessionId sessionId, BookingId id) {
        return new Booking(
                new BookingId(sessionId, new ParticipantId(UUID.randomUUID())),
                new SubscriptionId(UUID.randomUUID()),
                Booking.Status.WAITING_LIST);
    }

    private Session sessionMock() {
        return sessionMock(new SessionId(UUID.randomUUID()));
    }
}
