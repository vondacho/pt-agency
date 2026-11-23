package ch.obya.pta.customer.infrastructure.web;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.jboss.resteasy.reactive.ResponseStatus;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;

import io.smallrye.mutiny.Uni;

import ch.obya.pta.customer.application.CustomerService;
import ch.obya.pta.customer.domain.entity.SubscriptionRequest;
import ch.obya.pta.customer.domain.vo.ArticleId;
import ch.obya.pta.customer.domain.vo.CustomerId;
import ch.obya.pta.customer.domain.vo.SubscriptionRequestId;

/**
 * Subscription requests of a customer. Internal API: pta-www relays the customer's own requests,
 * the agency confirms them once paid.
 */
@Produces({"application/json", "application/problem+json"})
@Path("/customers/{id}/subscription-requests")
public class SubscriptionRequestResource {

    /**
     * @param subscription the subscription created by the confirmation, if confirmed
     */
    public record SubscriptionRequestDto(UUID id, UUID articleId, Instant requestedAt, String status, UUID subscription) {
        static SubscriptionRequestDto from(SubscriptionRequest request) {
            return new SubscriptionRequestDto(request.id().id(), request.articleId().id(), request.requestedAt(),
                    request.status().name(), request.subscription() == null ? null : request.subscription().id());
        }
    }

    public record NewRequest(UUID articleId) {
    }

    @Inject
    CustomerService service;

    /** All the customer's requests, latest first. */
    @GET
    public Uni<List<SubscriptionRequestDto>> list(@RestPath UUID id) {
        return service.findOne(new CustomerId(id))
                .map(c -> c.requests().stream()
                        .sorted(Comparator.comparing(SubscriptionRequest::requestedAt).reversed())
                        .map(SubscriptionRequestDto::from)
                        .toList());
    }

    @POST
    @ResponseStatus(201)
    public Uni<SubscriptionRequestDto> request(@RestPath UUID id, NewRequest input) {
        if (input == null || input.articleId() == null) {
            throw new jakarta.ws.rs.BadRequestException("articleId is required");
        }
        return service.requestSubscription(new CustomerId(id), new ArticleId(input.articleId()))
                .map(SubscriptionRequestDto::from);
    }

    /** The customer cancels a pending request. */
    @DELETE
    @Path("/{request}")
    public Uni<SubscriptionRequestDto> cancel(@RestPath UUID id, @RestPath UUID request) {
        return service.cancelSubscriptionRequest(new CustomerId(id), new SubscriptionRequestId(request))
                .map(SubscriptionRequestDto::from);
    }

    /** The agency confirms a request once paid: the subscription starts on {@code from} (default today). */
    @POST
    @Path("/{request}/confirmation")
    public Uni<SubscriptionRequestDto> confirm(@RestPath UUID id, @RestPath UUID request, @RestQuery LocalDate from) {
        return service.confirmSubscriptionRequest(new CustomerId(id), new SubscriptionRequestId(request), from)
                .map(SubscriptionRequestDto::from);
    }
}
