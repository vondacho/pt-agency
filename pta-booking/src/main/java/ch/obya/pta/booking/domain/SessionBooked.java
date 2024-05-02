package ch.obya.pta.booking.domain;

import java.util.Objects;

public record SessionBooked(SessionId session, ParticipantId participant) implements DomainEvent {
    public SessionBooked {
        Objects.requireNonNull(session);
        Objects.requireNonNull(participant);
    }
}
