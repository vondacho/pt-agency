package ch.obya.pta.booking.domain.repository;

import java.time.LocalDate;
import java.util.List;

import io.smallrye.mutiny.Uni;

import ch.obya.pta.booking.domain.aggregate.Session;
import ch.obya.pta.booking.domain.vo.ParticipantId;
import ch.obya.pta.booking.domain.vo.SessionId;
import ch.obya.pta.common.domain.repository.EntityRepository;

public interface SessionRepository extends EntityRepository<Session, SessionId, Session.State> {

    /**
     * Sessions from the given day on where the participant has a booking (booked, pre-booked or waiting),
     * in chronological order.
     */
    Uni<List<Session>> findBookedBy(ParticipantId participant, LocalDate from);
}
