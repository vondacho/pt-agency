package ch.obya.pta.shopping.infrastructure.web;

import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import ch.obya.pta.common.domain.util.Problem;

/**
 * Maps domain problems to HTTP problem documents: unknown entities are 404, conflicts 409, invalid input 400.
 */
public class ProblemExceptionMapper {

    @ServerExceptionMapper
    public Response map(Problem.Exception problem) {
        var status = switch (problem.code()) {
            case "EntityNotFound", "NoCustomer" -> Response.Status.NOT_FOUND;
            case "AlreadyExisting" -> Response.Status.CONFLICT;
            default -> Response.Status.BAD_REQUEST;
        };
        return Response.status(status)
                .type("application/problem+json")
                .entity(Map.of(
                        "title", status.getReasonPhrase(),
                        "status", status.getStatusCode(),
                        "code", problem.code(),
                        "detail", problem.getMessage()))
                .build();
    }
}
