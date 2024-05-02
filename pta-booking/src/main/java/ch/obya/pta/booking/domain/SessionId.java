package ch.obya.pta.booking.domain;

import java.util.Objects;
import java.util.UUID;

public record SessionId(UUID id) {
    public SessionId {
        Objects.requireNonNull(id);
    }
}
