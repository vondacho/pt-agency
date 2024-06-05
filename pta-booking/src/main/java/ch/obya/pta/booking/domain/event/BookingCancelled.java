package ch.obya.pta.booking.domain.event;


import ch.obya.pta.booking.domain.vo.ParticipantId;
import ch.obya.pta.booking.domain.vo.SessionId;
import ch.obya.pta.common.domain.event.Event;
import ch.obya.pta.common.domain.util.CommonProblem;

import java.time.Instant;

import static ch.obya.pta.common.domain.util.CommonProblem.ifNullThrow;
import static java.util.Optional.ofNullable;

public record BookingCancelled(SessionId session, ParticipantId participant, Instant timestamp) implements Event {
    public BookingCancelled {
        ifNullThrow(session, CommonProblem.AttributeNotNull.toException("BookingCancelled.session"));
        ifNullThrow(participant, CommonProblem.AttributeNotNull.toException("BookingCancelled.participant"));
        timestamp = ofNullable(timestamp).orElseGet(Instant::now);
    }

    public BookingCancelled(SessionId session, ParticipantId participant) {
        this(session, participant, null);
    }

}
