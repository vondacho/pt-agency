package ch.obya.pta.www.booking;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestQuery;

import io.smallrye.mutiny.Uni;

/**
 * Sessions offered to participants, relayed from pta-booking (the source of truth).
 * Served under {@code /api/sessions}.
 */
@Path("/sessions")
@Produces(MediaType.APPLICATION_JSON)
public class SessionResource {

    private static final Logger LOG = Logger.getLogger(SessionResource.class);

    @RestClient
    BookingClient booking;

    @GET
    public Uni<List<SessionView>> list(@RestQuery LocalDate from, @RestQuery LocalDate to) {
        return booking.sessions(from, to)
                .onFailure().transform(SessionResource::toClientError);
    }

    /**
     * Client errors from booking (e.g. an invalid period) are relayed as is;
     * anything else means booking is unavailable to us.
     */
    private static Throwable toClientError(Throwable failure) {
        if (failure instanceof WebApplicationException wae && wae.getResponse().getStatus() < 500) {
            return problem(Response.Status.fromStatusCode(wae.getResponse().getStatus()),
                    "The requested period is not valid.");
        }
        LOG.warnf("pta-booking unavailable: %s", failure.toString());
        return problem(Response.Status.BAD_GATEWAY, "Sessions are temporarily unavailable.");
    }

    private static WebApplicationException problem(Response.StatusType status, String detail) {
        return new WebApplicationException(Response.status(status)
                .type("application/problem+json")
                .entity(Map.of("title", status.getReasonPhrase(), "status", status.getStatusCode(), "detail", detail))
                .build());
    }
}
