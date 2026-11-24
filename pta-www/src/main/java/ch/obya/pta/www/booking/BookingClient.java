package ch.obya.pta.www.booking;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;

import io.smallrye.mutiny.Uni;

/**
 * Sessions and bookings, as held by pta-booking.
 */
@RegisterRestClient(configKey = "booking")
@Path("/sessions")
public interface BookingClient {

    /**
     * @param participant when given, each session tells the status of that participant's booking
     */
    @GET
    Uni<List<SessionView>> sessions(@RestQuery LocalDate from, @RestQuery LocalDate to, @RestQuery UUID participant);

    /** The session with its instructor, venue, note and cancellation deadline. */
    @GET
    @Path("/{session}/details")
    Uni<SessionDetails> details(@RestPath UUID session, @RestQuery UUID participant);

    /** pta-booking's details of a session. */
    record SessionDetails(SessionView session, UUID articleId, Instructor instructor, Venue venue, String note,
                          Integer cancellationHours, java.time.LocalDateTime cancellableUntil) {
    }

    record Instructor(UUID id, String name, String bio) {
    }

    record Venue(UUID id, String name, String address, String accessNotes, String mapUrl) {
    }

    @POST
    @Path("/{session}/bookings/{participant}")
    Uni<BookingView> book(@RestPath UUID session, @RestPath UUID participant);

    @DELETE
    @Path("/{session}/bookings/{participant}")
    Uni<Void> cancel(@RestPath UUID session, @RestPath UUID participant);
}
