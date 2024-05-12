package ch.obya.pta.customer.domain.event;

import ch.obya.pta.common.domain.event.Event;
import ch.obya.pta.common.domain.util.CommonProblem;
import ch.obya.pta.customer.domain.vo.CustomerId;

import java.time.Instant;

import static ch.obya.pta.common.domain.util.CommonProblem.ifNullThrow;
import static java.util.Optional.ofNullable;

public record CustomerModified(CustomerId id, Instant timestamp) implements Event {

    public CustomerModified {
        ifNullThrow(id, CommonProblem.AttributeNotNull.toException("%s.id".formatted(getClass().getSimpleName())));
        timestamp = ofNullable(timestamp).orElseGet(Instant::now);
    }

    public CustomerModified(CustomerId id) {
        this(id, null);
    }
}
