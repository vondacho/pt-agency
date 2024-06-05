package ch.obya.pta.booking.domain.event;


import ch.obya.pta.booking.domain.vo.ParticipantId;
import ch.obya.pta.booking.domain.vo.SessionId;
import ch.obya.pta.common.domain.event.Event;
import ch.obya.pta.common.domain.util.CommonProblem;

import java.time.Instant;

import static ch.obya.pta.common.domain.util.CommonProblem.ifNullThrow;
import static java.util.Optional.ofNullable;

public record ParticipantWaitlisted(SessionId session, ParticipantId participant, Instant timestamp) implements Event {
    public ParticipantWaitlisted {
        ifNullThrow(session, CommonProblem.AttributeNotNull.toException("ParticipantWaitlisted.session"));
        ifNullThrow(participant, CommonProblem.AttributeNotNull.toException("ParticipantWaitlisted.participant"));
        timestamp = ofNullable(timestamp).orElseGet(Instant::now);
    }

    public ParticipantWaitlisted(SessionId session, ParticipantId participant) {
        this(session, participant, null);
    }
}
