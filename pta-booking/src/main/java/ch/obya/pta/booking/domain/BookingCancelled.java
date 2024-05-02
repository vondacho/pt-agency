package ch.obya.pta.booking.domain;

import java.util.Objects;

public record BookingCancelled(SessionId session, ParticipantId participant) implements DomainEvent {
    public BookingCancelled {
        Objects.requireNonNull(session);
        Objects.requireNonNull(participant);
    }
}
