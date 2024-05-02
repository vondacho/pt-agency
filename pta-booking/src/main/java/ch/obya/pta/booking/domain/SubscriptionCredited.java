package ch.obya.pta.booking.domain;

import java.util.Objects;

public record SubscriptionCredited(SubscriptionId subscription, ParticipantId participant) implements DomainEvent {
    public SubscriptionCredited {
        Objects.requireNonNull(subscription);
        Objects.requireNonNull(participant);
    }
}
