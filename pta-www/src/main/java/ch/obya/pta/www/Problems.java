package ch.obya.pta.www;

import java.util.Map;
import java.util.function.IntFunction;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.jboss.logging.Logger;

/**
 * Problem documents for the SPA, and the relay of the services' answers: their client errors become messages
 * for the participant, anything else means the service is unavailable.
 */
public final class Problems {

    private static final Logger LOG = Logger.getLogger(Problems.class);

    private Problems() {
    }

    public static WebApplicationException problem(int status, String detail) {
        var known = Response.Status.fromStatusCode(status);
        var title = known != null ? known.getReasonPhrase() : status == 422 ? "Unprocessable Content" : "Error";
        return new WebApplicationException(Response.status(status)
                .type("application/problem+json")
                .entity(Map.of("title", title, "status", status, "detail", detail))
                .build());
    }

    /**
     * @param messages    participant-facing message for a client error status of the service, or null for a generic one
     * @param unavailable message when the service cannot answer, e.g. "Sessions are temporarily unavailable."
     */
    public static Throwable relay(Throwable failure, IntFunction<String> messages, String unavailable) {
        if (failure instanceof WebApplicationException wae && wae.getResponse().getStatus() < 500) {
            if (wae.getResponse().getEntity() instanceof Map<?, ?>) {
                return wae; // already a problem of ours
            }
            int status = wae.getResponse().getStatus();
            var message = messages.apply(status);
            return problem(status, message != null ? message : "The request is not valid.");
        }
        LOG.warnf("service unavailable (%s): %s", unavailable, failure.toString());
        return problem(502, unavailable);
    }
}
