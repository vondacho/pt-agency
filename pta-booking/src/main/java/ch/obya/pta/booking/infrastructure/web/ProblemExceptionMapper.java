package ch.obya.pta.booking.infrastructure.web;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import ch.obya.pta.common.domain.util.Problem;

/**
 * Maps failures to HTTP problem documents. The {@code code} member lets clients react to a specific problem.
 */
public class ProblemExceptionMapper {

    /** Takes precedence over the generic mappers of quarkus-resteasy-problem, which would answer 500. */
    static final int BEFORE_LIBRARY_MAPPERS = Priorities.USER - 100;

    @ServerExceptionMapper(priority = BEFORE_LIBRARY_MAPPERS)
    public Response map(Problem.Exception problem) {
        var status = switch (problem.code()) {
            case "EntityNotFound", "NoSession", "NoBooking", "NoSessionForBooking" -> 404;
            case "NoActiveSubscription", "SubscriptionStillNotActive", "SubscriptionNoMoreActive", "CancellationClosed" -> 422;
            case "SessionQuotaReached", "SubscriptionQuotaReached", "ConcurrentModification" -> 409;
            case "ServiceUnavailable" -> 502;
            default -> 400;
        };
        return problem(status, problem.code(), problem.getMessage());
    }

    private static Response problem(int status, String code, String detail) {
        var known = Response.Status.fromStatusCode(status);
        var title = known != null ? known.getReasonPhrase() : status == 422 ? "Unprocessable Content" : "Error";
        var body = new LinkedHashMap<String, Object>(Map.of("title", title, "status", status, "code", code, "detail", detail));
        return Response.status(status).type("application/problem+json").entity(body).build();
    }
}
