package ch.obya.pta.booking.infrastructure.web;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;

import ch.obya.pta.booking.domain.repository.SessionRepository;
import ch.obya.pta.booking.domain.vo.ParticipantId;

/**
 * A participant's view of their bookings.
 */
@Produces({"application/json", "application/problem+json"})
@Path("/participants")
public class ParticipantResource {

    @Inject
    SessionRepository sessionRepository;

    /**
     * Sessions from {@code from} (default today) where the participant is booked, pre-booked or on the waiting list,
     * in chronological order; each tells the participant's booking status.
     */
    @GET
    @Path("/{id}/bookings")
    @WithSession
    public Uni<List<SessionDto>> bookings(@RestPath UUID id, @RestQuery LocalDate from) {
        var participant = new ParticipantId(id);
        return sessionRepository.findBookedBy(participant, Optional.ofNullable(from).orElseGet(LocalDate::now))
                .map(sessions -> sessions.stream().map(s -> SessionDto.from(s, participant)).toList());
    }
}
