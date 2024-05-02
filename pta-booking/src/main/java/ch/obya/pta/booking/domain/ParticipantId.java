package ch.obya.pta.booking.domain;

import java.util.Objects;
import java.util.UUID;

public record ParticipantId(UUID id) {
    public ParticipantId {
        Objects.requireNonNull(id);
    }
}
