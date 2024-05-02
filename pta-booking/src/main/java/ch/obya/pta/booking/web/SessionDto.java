package ch.obya.pta.booking.web;

import ch.obya.pta.booking.domain.Session;

import java.util.UUID;

public record SessionDto(UUID id) {

    public static SessionDto from(Session session) {
        return new SessionDto(session.id().id());
    }
}
