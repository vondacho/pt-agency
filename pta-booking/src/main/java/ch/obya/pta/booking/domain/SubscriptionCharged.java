package ch.obya.pta.booking.domain;

import java.util.Objects;

public record SubscriptionCharged(SubscriptionId subscription, ParticipantId participant) implements DomainEvent {
    public SubscriptionCharged {
        Objects.requireNonNull(subscription);
        Objects.requireNonNull(participant);
    }
}
