package ch.obya.pta.www.me;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;

import io.smallrye.mutiny.Uni;

import ch.obya.pta.www.booking.SessionView;

/**
 * A participant's bookings, as held by pta-booking.
 */
@RegisterRestClient(configKey = "booking")
@Path("/participants")
public interface ParticipantBookingsClient {

    @GET
    @Path("/{id}/bookings")
    Uni<List<SessionView>> bookings(@RestPath UUID id, @RestQuery LocalDate from);
}
