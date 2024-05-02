package ch.obya.pta.booking.domain;

import java.util.Objects;
import java.util.UUID;

public record SubscriptionId(UUID id) {
    public SubscriptionId {
        Objects.requireNonNull(id);
    }
}
