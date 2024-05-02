package ch.obya.pta.booking.data;

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
