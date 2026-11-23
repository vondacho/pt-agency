package ch.obya.pta.customer.infrastructure.web;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import ch.obya.pta.common.domain.util.Problem;

/**
 * Maps domain problems to HTTP problem documents; the {@code code} member lets clients react to a specific problem.
 */
public class ProblemExceptionMapper {

    /** Takes precedence over the generic mappers of quarkus-resteasy-problem. */
    static final int BEFORE_LIBRARY_MAPPERS = Priorities.USER - 100;

    @ServerExceptionMapper(priority = BEFORE_LIBRARY_MAPPERS)
    public Response map(Problem.Exception problem) {
        var status = switch (problem.code()) {
            case "EntityNotFound", "NoCustomer" -> 404;
            case "AlreadyExisting", "RequestAlreadyPending", "SubscriptionOverlapsExisting", "RequestOfOtherArticle" -> 409;
            case "RequestNotPending", "UnknownArticle" -> 422;
            case "ServiceUnavailable" -> 502;
            default -> 400;
        };
        var known = Response.Status.fromStatusCode(status);
        var title = known != null ? known.getReasonPhrase() : "Unprocessable Content";
        var body = new LinkedHashMap<String, Object>(Map.of(
                "title", title, "status", status, "code", problem.code(), "detail", problem.getMessage()));
        return Response.status(status).type("application/problem+json").entity(body).build();
    }
}
