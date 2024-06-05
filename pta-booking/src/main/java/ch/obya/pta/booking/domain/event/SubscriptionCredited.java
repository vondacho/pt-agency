package ch.obya.pta.booking.domain.event;


import ch.obya.pta.booking.domain.vo.ParticipantId;
import ch.obya.pta.booking.domain.vo.SubscriptionId;
import ch.obya.pta.common.domain.event.Event;
import ch.obya.pta.common.domain.util.CommonProblem;

import java.time.Instant;

import static ch.obya.pta.common.domain.util.CommonProblem.ifNullThrow;
import static java.util.Optional.ofNullable;

public record SubscriptionCredited(SubscriptionId subscription, ParticipantId participant, Instant timestamp) implements Event {
    public SubscriptionCredited {
        ifNullThrow(subscription, CommonProblem.AttributeNotNull.toException("SubscriptionCredited.subscription"));
        ifNullThrow(participant, CommonProblem.AttributeNotNull.toException("SubscriptionCredited.participant"));
        timestamp = ofNullable(timestamp).orElseGet(Instant::now);
    }

    public SubscriptionCredited(SubscriptionId subscription, ParticipantId participant) {
        this(subscription, participant, null);
    }
}
