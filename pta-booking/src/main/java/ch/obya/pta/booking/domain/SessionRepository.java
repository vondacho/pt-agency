package ch.obya.pta.booking.domain;

import io.smallrye.mutiny.Uni;

import java.time.LocalDate;
import java.util.List;

public interface SessionRepository {

    Uni<Session> findById(SessionId sessionId);
    Uni<List<Session>> findAllFromTo(LocalDate from, LocalDate to);
    Uni<Session> findByBookingId(BookingId booking);
    Uni<Session> persist(Session session);

}
