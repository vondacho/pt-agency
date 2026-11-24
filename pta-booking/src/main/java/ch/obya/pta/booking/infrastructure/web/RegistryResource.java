package ch.obya.pta.booking.infrastructure.web;

import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.jboss.resteasy.reactive.RestPath;

import io.smallrye.mutiny.Uni;

import ch.obya.pta.booking.domain.repository.InstructorRepository;
import ch.obya.pta.booking.domain.repository.VenueRepository;
import ch.obya.pta.booking.domain.vo.InstructorId;
import ch.obya.pta.booking.domain.vo.VenueId;
import ch.obya.pta.common.domain.util.CommonProblem;

/**
 * Instructors and venues (read-only).
 */
@Produces({"application/json", "application/problem+json"})
@Path("/")
public class RegistryResource {

    @Inject
    InstructorRepository instructors;

    @Inject
    VenueRepository venues;

    @GET
    @Path("instructors/{id}")
    public Uni<SessionDetailsDto.InstructorDto> instructor(@RestPath UUID id) {
        return instructors.findOne(new InstructorId(id))
                .onItem().ifNull().failWith(CommonProblem.EntityNotFound.toException("Instructor", id))
                .map(SessionDetailsDto.InstructorDto::from);
    }

    @GET
    @Path("venues/{id}")
    public Uni<SessionDetailsDto.VenueDto> venue(@RestPath UUID id) {
        return venues.findOne(new VenueId(id))
                .onItem().ifNull().failWith(CommonProblem.EntityNotFound.toException("Venue", id))
                .map(SessionDetailsDto.VenueDto::from);
    }
}
