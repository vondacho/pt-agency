package ch.obya.pta.booking.infrastructure.web;


import ch.obya.pta.booking.domain.aggregate.Session;
import ch.obya.pta.booking.domain.vo.SessionId;

public record SessionDto(SessionId id) {
    public static SessionDto from(Session session) {
        return new SessionDto(session.id());
    }
}
