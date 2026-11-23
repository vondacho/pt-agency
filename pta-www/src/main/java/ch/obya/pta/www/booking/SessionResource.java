package ch.obya.pta.www.booking;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.ResponseStatus;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;

import io.smallrye.mutiny.Uni;

import ch.obya.pta.www.auth.Participant;

/**
 * Sessions offered to participants and their own bookings, relayed to pta-booking (the source of truth).
 * Served under {@code /api/sessions}.
 */
@Path("/sessions")
@Produces(MediaType.APPLICATION_JSON)
public class SessionResource {

    private static final Logger LOG = Logger.getLogger(SessionResource.class);

    /**
     * Required on state-changing requests. A cross-site page cannot send a custom header without a CORS preflight,
     * which pta-www never grants: together with the Lax session cookie, this protects bookings from CSRF.
     */
    static final String REQUEST_HEADER = "X-PTA-Request";

    @RestClient
    BookingClient booking;

    @Inject
    Participant participant;

    /**
     * Sessions of the period; for a signed-in participant, each one tells the status of their booking.
     */
    @GET
    public Uni<List<SessionView>> list(@RestQuery LocalDate from, @RestQuery LocalDate to) {
        return booking.sessions(from, to, participant.id().orElse(null))
                .onFailure().transform(SessionResource::toClientError);
    }

    /**
     * Books a seat (or a place on the waiting list) for the signed-in participant.
     */
    @POST
    @Path("/{id}/booking")
    @ResponseStatus(201)
    public Uni<BookingView.Result> book(@RestPath UUID id, @HeaderParam(REQUEST_HEADER) String requestHeader) {
        return booking.book(id, authorizedParticipant(requestHeader))
                .map(BookingView::toResult)
                .onFailure().transform(SessionResource::toClientError);
    }

    /**
     * Cancels the signed-in participant's booking, or leaves the waiting list.
     */
    @DELETE
    @Path("/{id}/booking")
    @ResponseStatus(204)
    public Uni<Void> cancel(@RestPath UUID id, @HeaderParam(REQUEST_HEADER) String requestHeader) {
        return booking.cancel(id, authorizedParticipant(requestHeader))
                .onFailure().transform(SessionResource::toClientError);
    }

    private UUID authorizedParticipant(String requestHeader) {
        if (!participant.signedIn()) {
            throw problem(Response.Status.UNAUTHORIZED, "Sign in to book a session.");
        }
        if (!"1".equals(requestHeader)) {
            throw problem(Response.Status.FORBIDDEN, "Missing " + REQUEST_HEADER + " header.");
        }
        return participant.id().orElseThrow(() ->
                problem(Response.Status.FORBIDDEN, "Your account is not linked to a participant yet. Please contact the agency."));
    }

    /**
     * Problems of pta-booking are relayed with a message for the participant; anything else means it is unavailable.
     */
    private static Throwable toClientError(Throwable failure) {
        if (failure instanceof WebApplicationException wae && wae.getResponse().getStatus() < 500) {
            var status = Response.Status.fromStatusCode(wae.getResponse().getStatus());
            int code = wae.getResponse().getStatus();
            return switch (code) {
                case 422 -> problem(422, "Unprocessable Content", "You have no active subscription valid for this session.");
                case 409 -> problem(409, "Conflict", "This session has just changed. Please try again.");
                case 404 -> problem(404, "Not Found", "This session or booking no longer exists.");
                default -> problem(code, status != null ? status.getReasonPhrase() : "Error", "The request is not valid.");
            };
        }
        LOG.warnf("pta-booking unavailable: %s", failure.toString());
        return problem(Response.Status.BAD_GATEWAY, "Sessions are temporarily unavailable.");
    }

    private static WebApplicationException problem(Response.Status status, String detail) {
        return problem(status.getStatusCode(), status.getReasonPhrase(), detail);
    }

    private static WebApplicationException problem(int status, String title, String detail) {
        return new WebApplicationException(Response.status(status)
                .type("application/problem+json")
                .entity(Map.of("title", title, "status", status, "detail", detail))
                .build());
    }
}
