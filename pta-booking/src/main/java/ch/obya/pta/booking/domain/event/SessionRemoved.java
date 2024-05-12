package ch.obya.pta.booking.domain.event;

import ch.obya.pta.booking.domain.vo.SessionId;
import ch.obya.pta.common.domain.event.Event;
import ch.obya.pta.common.domain.util.CommonProblem;

import java.time.Instant;

import static ch.obya.pta.common.domain.util.CommonProblem.ifNullThrow;
import static java.util.Optional.ofNullable;

public record SessionRemoved(SessionId id, Instant timestamp) implements Event {

    public SessionRemoved {
        ifNullThrow(id, CommonProblem.AttributeNotNull.toException("%s.id".formatted(getClass().getSimpleName())));
        timestamp = ofNullable(timestamp).orElseGet(Instant::now);
    }

    public SessionRemoved(SessionId id) {
        this(id, null);
    }
}
