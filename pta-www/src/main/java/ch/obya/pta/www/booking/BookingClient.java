package ch.obya.pta.www.booking;

import java.time.LocalDate;
import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestQuery;

import io.smallrye.mutiny.Uni;

/**
 * Read access to the sessions published by pta-booking.
 */
@RegisterRestClient(configKey = "booking")
@Path("/sessions")
public interface BookingClient {

    @GET
    Uni<List<SessionView>> sessions(@RestQuery LocalDate from, @RestQuery LocalDate to);
}
