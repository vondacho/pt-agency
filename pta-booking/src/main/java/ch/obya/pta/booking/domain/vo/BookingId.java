package ch.obya.pta.booking.domain.vo;


import ch.obya.pta.common.domain.util.CommonProblem;

import static ch.obya.pta.common.domain.util.CommonProblem.ifNullThrow;

public record BookingId(SessionId session, ParticipantId participant) {
    public BookingId {
        ifNullThrow(session, CommonProblem.AttributeNotNull.toException("BookingId.session"));
        ifNullThrow(participant, CommonProblem.AttributeNotNull.toException("BookingId.participant"));
    }

    public boolean equals(SessionId session, ParticipantId participant) {
        return this.session.equals(session) && this.participant.equals(participant);
    }
}
