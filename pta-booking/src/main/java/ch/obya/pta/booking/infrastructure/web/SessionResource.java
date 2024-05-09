package ch.obya.pta.booking.infrastructure.web;

/*-
 * #%L
 * pta-booking
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2024 obya
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import ch.obya.pta.booking.application.BookingService;
import ch.obya.pta.booking.domain.vo.BookingId;
import ch.obya.pta.booking.domain.vo.ParticipantId;
import ch.obya.pta.booking.domain.vo.SessionId;
import ch.obya.pta.booking.domain.repository.SessionRepository;
import io.quarkus.resteasy.reactive.links.InjectRestLinks;
import io.quarkus.resteasy.reactive.links.RestLink;
import io.quarkus.resteasy.reactive.links.RestLinkType;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import org.jboss.resteasy.reactive.ResponseStatus;
import org.jboss.resteasy.reactive.RestQuery;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

@Path("/sessions")
@Produces("application/problem+json")
public class SessionResource {

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
    @InjectRestLinks(RestLinkType.INSTANCE)
    @RestLink(rel = "self")
    @POST
    @Path("{id}")
    public Uni<SessionId> create() {
        return Uni.createFrom().nothing();
    }

    @InjectRestLinks(RestLinkType.INSTANCE)
    @RestLink(rel = "self")
    @PUT
    @Path("{id}")
    public Uni<SessionId> update(SessionId id) {
        return Uni.createFrom().nothing();
    }

    @DELETE
    @Path("{id}")
    public Uni<Void> remove(SessionId id) {
        return Uni.createFrom().nothing();
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
}