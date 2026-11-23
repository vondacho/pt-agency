package ch.obya.pta.booking.infrastructure.web;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;
import jakarta.ws.rs.*;

import org.hibernate.StaleStateException;
import org.jboss.resteasy.reactive.ResponseStatus;
import org.jboss.resteasy.reactive.RestQuery;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;

import io.quarkus.resteasy.reactive.links.InjectRestLinks;
import io.quarkus.resteasy.reactive.links.RestLink;
import io.quarkus.resteasy.reactive.links.RestLinkType;
import io.smallrye.mutiny.Uni;

import ch.obya.pta.booking.application.BookingService;
import ch.obya.pta.booking.domain.repository.SessionRepository;
import ch.obya.pta.booking.domain.util.BookingProblem;
import ch.obya.pta.booking.domain.vo.ParticipantId;
import ch.obya.pta.booking.domain.vo.SessionId;

import static ch.obya.pta.common.util.search.AttributeFilter.greaterThanOrEqual;
import static ch.obya.pta.common.util.search.AttributeFilter.lessThanOrEqual;

@Produces({"application/json", "application/problem+json"})
@Path("/sessions")
public class SessionResource {

    @Inject
    BookingService bookingService;
    @Inject
    SessionRepository sessionRepository;

    static final int DEFAULT_PERIOD_DAYS = 7;

    /**
     * Lists the sessions taking place between {@code from} and {@code to} (inclusive), in chronological order.
     * Defaults to the coming {@value DEFAULT_PERIOD_DAYS} days starting today.
     * With {@code participant}, each session tells the status of that participant's booking, if any.
     */
    @GET
    public Uni<List<SessionDto>> list(@RestQuery LocalDate from, @RestQuery LocalDate to, @RestQuery UUID participant) {
        var start = Optional.ofNullable(from).orElseGet(LocalDate::now);
        var end = Optional.ofNullable(to).orElseGet(() -> start.plusDays(DEFAULT_PERIOD_DAYS));
        if (end.isBefore(start)) {
            return Uni.createFrom().failure(Problem.valueOf(Status.BAD_REQUEST, "'to' must not be before 'from'"));
        }
        return sessionRepository.findByCriteria(Map.of("day", List.of(greaterThanOrEqual(start), lessThanOrEqual(end))))
                .map(sessions -> sessions.stream()
                        .map(session -> SessionDto.from(session, participant == null ? null : new ParticipantId(participant)))
                        .toList());
    }

    @InjectRestLinks(RestLinkType.INSTANCE)
    @RestLink(rel = "self")
    @GET
    @Path("{id}")
    public Uni<SessionDto> get(SessionId id) {
        return bookingService.findOne(id).map(SessionDto::from);
    }

    @ResponseStatus(201)
    @InjectRestLinks(RestLinkType.INSTANCE)
    @RestLink(rel = "self")
    @POST
    @Path("{id}")
    public Uni<SessionId> create() {
        //TODO
        return Uni.createFrom().nothing();
    }

    @InjectRestLinks(RestLinkType.INSTANCE)
    @RestLink(rel = "self")
    @PUT
    @Path("{id}")
    public Uni<SessionId> update(SessionId id) {
        //TODO
        return Uni.createFrom().nothing();
    }

    @DELETE
    @Path("{id}")
    public Uni<Void> remove(SessionId id) {
        return bookingService.remove(id);
    }

    @ResponseStatus(201)
    @POST
    @Path("{id}/bookings/{participant}")
    public Uni<BookingDto> book(SessionId id, ParticipantId participant) {
        return conflictOnConcurrentChange(id, bookingService.book(id, participant)).map(BookingDto::from);
    }

    @ResponseStatus(204)
    @DELETE
    @Path("{id}/bookings/{participant}")
    public Uni<Void> cancel(SessionId id, ParticipantId participant) {
        return conflictOnConcurrentChange(id, bookingService.cancel(id, participant));
    }

    @GET
    @Path("{id}/bookings")
    public Uni<List<BookingDto>> listBookings(SessionId id) {
        return bookingService.findOne(id)
                .map(s -> s.bookings().stream().map(BookingDto::from).toList());
    }

    /**
     * A session saved by another booking since it was read fails at commit (optimistic locking), after the
     * service's own pipeline: translate it here, where the committed result is observed.
     */
    private static <T> Uni<T> conflictOnConcurrentChange(SessionId id, Uni<T> booking) {
        return booking
                .onFailure(f -> f instanceof OptimisticLockException || f instanceof StaleStateException)
                .transform(f -> BookingProblem.ConcurrentModification.toException(id.id()).get());
    }
}
