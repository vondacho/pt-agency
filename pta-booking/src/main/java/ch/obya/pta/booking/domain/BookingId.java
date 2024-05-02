package ch.obya.pta.booking.domain;

import java.util.Objects;

public record BookingId(SessionId session, ParticipantId participant) {
    public BookingId {
        Objects.requireNonNull(session);
        Objects.requireNonNull(participant);
    }

    public boolean equals(SessionId session, ParticipantId participant) {
        return this.session.equals(session) && this.participant.equals(participant);
    }
}
