package ch.obya.pta.customer.domain.event;

import ch.obya.pta.common.domain.event.Event;
import ch.obya.pta.common.util.exception.CommonProblem;
import ch.obya.pta.customer.domain.vo.CustomerId;

import java.time.Instant;

import static ch.obya.pta.common.util.exception.CommonProblem.ifNullThrow;
import static java.util.Optional.ofNullable;

public record CustomerCreated(CustomerId id, Instant timestamp) implements Event {
    public CustomerCreated {
        ifNullThrow(id, CommonProblem.AttributeNotNull.toException("CustomerCreated.id"));
        ifNullThrow(timestamp, CommonProblem.AttributeNotNull.toException("CustomerCreated.timestamp"));
        timestamp = ofNullable(timestamp).orElseGet(Instant::now);
    }

    public CustomerCreated(CustomerId id) {
        this(id, null);
    }
}
