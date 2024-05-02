package ch.obya.pta.booking.web;

import ch.obya.pta.booking.application.BookingService;
import ch.obya.pta.booking.domain.BookingId;
import ch.obya.pta.booking.domain.ParticipantId;
import ch.obya.pta.booking.domain.SessionId;
import ch.obya.pta.booking.domain.SessionRepository;
import io.quarkus.resteasy.reactive.links.InjectRestLinks;
import io.quarkus.resteasy.reactive.links.RestLink;
import io.quarkus.resteasy.reactive.links.RestLinkType;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import org.jboss.resteasy.reactive.ResponseStatus;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Path("/sessions")
@Produces("application/problem+json")
public class BookingResource {

    @Inject
    BookingService bookingService;
    @Inject
    SessionRepository sessionRepository;

    @InjectRestLinks
    @RestLink
    @GET
    public Uni<List<SessionDto>> list(@RestQuery LocalDate from, @RestQuery LocalDate to) {
        return sessionRepository.findAllFromTo(from, to)
                .onItem().transform(it -> it.stream().map(SessionDto::from).toList())
                .ifNoItem().after(Duration.ofMillis(100)).fail();
    }

    @InjectRestLinks(RestLinkType.INSTANCE)
    @RestLink(rel = "self")
    @GET
    @Path("{id}")
    public Uni<SessionDto> get(SessionId id) {
        return sessionRepository.findById(id)
                .onItem().transform(SessionDto::from)
                .ifNoItem().after(Duration.ofMillis(100))
                .failWith(() -> Problem.valueOf(Status.NOT_FOUND, "Session %s does not exist".formatted(id)));
    }

    @ResponseStatus(201)
    @POST
    @Path("{id}/bookings/{participant}")
    public Uni<BookingId> book(SessionId id, ParticipantId participant) {
        return bookingService.book(id, participant);
    }

    @ResponseStatus(204)
    @DELETE
    @Path("{id}/bookings/{participant}")
    public Uni<Void> cancel(SessionId id, ParticipantId participant) {
        return bookingService.cancel(id, participant);
    }

    @GET
    @Path("{id}/bookings")
    public Multi<BookingDto> listBookings(SessionId id) {
        return sessionRepository.findById(id)
                .ifNoItem().after(Duration.ofMillis(100))
                .failWith(() -> Problem.valueOf(Status.NOT_FOUND, "Session %s does not exist".formatted(id)))
                .onItem().transformToMulti(s -> Multi.createFrom().iterable(s.bookings()))
                .onItem().transform(BookingDto::from);
    }

    @GET
    @Path("/bookings")
    public Multi<BookingDto> listAllBookings(@RestQuery LocalDate from, @RestQuery LocalDate to) {
        return Multi.createFrom().nothing();
    }
}
