package ch.obya.pta.booking.domain;

import java.util.Objects;

public record ParticipantWaitlisted(SessionId session, ParticipantId participant) implements DomainEvent {
    public ParticipantWaitlisted {
        Objects.requireNonNull(session);
        Objects.requireNonNull(participant);
    }
}
