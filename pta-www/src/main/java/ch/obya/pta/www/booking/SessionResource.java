package ch.obya.pta.www.booking;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ResponseStatus;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;

import io.smallrye.mutiny.Uni;

import ch.obya.pta.www.Problems;
import ch.obya.pta.www.auth.Participant;

/**
 * Sessions offered to participants and their own bookings, relayed to pta-booking (the source of truth).
 * Served under {@code /api/sessions}.
 */
@Path("/sessions")
@Produces(MediaType.APPLICATION_JSON)
public class SessionResource {

    static final String REQUEST_HEADER = Participant.REQUEST_HEADER;

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
        return booking.book(id, participant.requireLinked(requestHeader))
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
        return booking.cancel(id, participant.requireLinked(requestHeader))
                .onFailure().transform(SessionResource::toClientError);
    }

    /**
     * Problems of pta-booking are relayed with a message for the participant.
     */
    private static Throwable toClientError(Throwable failure) {
        return Problems.relay(failure, status -> switch (status) {
            case 422 -> "You have no active subscription valid for this session.";
            case 409 -> "This session has just changed. Please try again.";
            case 404 -> "This session or booking no longer exists.";
            default -> null;
        }, "Sessions are temporarily unavailable.");
    }
}
