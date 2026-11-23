package ch.obya.pta.www.me;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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

import io.smallrye.mutiny.Uni;

import ch.obya.pta.www.Problems;
import ch.obya.pta.www.auth.Participant;
import ch.obya.pta.www.booking.SessionView;
import ch.obya.pta.www.shop.Catalogue;
import ch.obya.pta.www.shop.ShoppingClient;

/**
 * The signed-in participant's bookings, subscriptions and subscription requests. Served under {@code /api/me}.
 */
@Path("/me")
@Produces(MediaType.APPLICATION_JSON)
public class MySpaceResource {

    /** Subscriptions ending within this many days are flagged. */
    static final int EXPIRY_WARNING_DAYS = 30;

    /**
     * @param sessions    sessions included, absent when unlimited
     * @param expiresSoon ends within {@value #EXPIRY_WARNING_DAYS} days
     * @param upcoming    not started yet
     */
    public record MySubscription(UUID id, UUID articleId, String name, LocalDate from, LocalDate to,
                                 Integer sessions, boolean expiresSoon, boolean upcoming) {
    }

    public record MyRequest(UUID id, UUID articleId, String name, Instant requestedAt, String status) {
    }

    public record MySubscriptions(List<MySubscription> subscriptions, List<MyRequest> requests) {
    }

    public record NewRequest(UUID articleId) {
    }

    @RestClient
    CustomerClient customers;

    @RestClient
    ParticipantBookingsClient bookings;

    @RestClient
    ShoppingClient shopping;

    @Inject
    Catalogue catalogue;

    @Inject
    Participant participant;

    /** Coming sessions the participant is booked on, pre-booked for or waiting for. */
    @GET
    @Path("/bookings")
    public Uni<List<SessionView>> bookings() {
        var id = participant.requireLinked();
        return bookings.bookings(id, LocalDate.now())
                .onFailure().transform(f -> Problems.relay(f, status -> null, "Your bookings are temporarily unavailable."));
    }

    /** Current and coming subscriptions, and pending requests. */
    @GET
    @Path("/subscriptions")
    public Uni<MySubscriptions> subscriptions() {
        var id = participant.requireLinked();
        var today = LocalDate.now();
        return articleNames().flatMap(names -> customers.subscriptions(id).flatMap(subscriptions -> customers.requests(id).map(requests ->
                new MySubscriptions(
                        subscriptions.stream()
                                .filter(s -> s.validity().to() == null || !s.validity().to().isBefore(today))
                                .sorted(Comparator.comparing(s -> s.validity().from()))
                                .map(s -> new MySubscription(s.id().id(), s.articleId().id(),
                                        names.getOrDefault(s.articleId().id(), "Subscription"),
                                        s.validity().from(), s.validity().to(),
                                        s.credits() == null || s.credits() == 0 ? null : s.credits(),
                                        s.validity().to() != null && !s.validity().to().isAfter(today.plusDays(EXPIRY_WARNING_DAYS)),
                                        s.validity().from().isAfter(today)))
                                .toList(),
                        requests.stream()
                                .filter(r -> "PENDING".equals(r.status()))
                                .map(r -> toRequest(r, names))
                                .toList()))))
                .onFailure().transform(f -> Problems.relay(f, status -> null, "Your subscriptions are temporarily unavailable."));
    }

    /** Requests a subscription or pass of the catalogue, to be confirmed by the agency. */
    @POST
    @Path("/subscription-requests")
    @ResponseStatus(201)
    public Uni<MyRequest> request(NewRequest request, @HeaderParam(Participant.REQUEST_HEADER) String requestHeader) {
        var id = participant.requireLinked(requestHeader);
        if (request == null || request.articleId() == null) {
            throw Problems.problem(400, "Please choose a subscription.");
        }
        return catalogue.item(request.articleId())
                .flatMap(item -> {
                    if (item.isEmpty()) {
                        return Uni.createFrom().failure(Problems.problem(422, "This subscription cannot be requested."));
                    }
                    return customers.request(id, new CustomerClient.NewRequest(request.articleId()))
                            .map(r -> toRequest(r, Map.of(item.get().id(), item.get().name())));
                })
                .onFailure().transform(f -> Problems.relay(f, status -> switch (status) {
                    case 409 -> "You have already requested this subscription.";
                    case 422 -> "This subscription cannot be requested.";
                    default -> null;
                }, "Requests are temporarily unavailable."));
    }

    /** Cancels a pending request. */
    @DELETE
    @Path("/subscription-requests/{request}")
    @ResponseStatus(204)
    public Uni<Void> cancel(@RestPath UUID request, @HeaderParam(Participant.REQUEST_HEADER) String requestHeader) {
        var id = participant.requireLinked(requestHeader);
        return customers.cancel(id, request).replaceWithVoid()
                .onFailure().transform(f -> Problems.relay(f, status -> switch (status) {
                    case 404 -> "This request no longer exists.";
                    case 422 -> "This request has already been handled by the agency.";
                    default -> null;
                }, "Requests are temporarily unavailable."));
    }

    /** Names of every subscription and pass, including those no longer on sale. */
    private Uni<Map<UUID, String>> articleNames() {
        return shopping.articles(List.of("SUBSCRIPTION", "PASS"), false)
                .map(articles -> articles.stream().collect(Collectors.toMap(a -> a.id().id(), a -> a.name().content(), (a, b) -> a)))
                .onFailure().recoverWithItem(Map.of()); // names are a nicety: subscriptions still show without them
    }

    private static MyRequest toRequest(CustomerClient.SubscriptionRequest r, Map<UUID, String> names) {
        return new MyRequest(r.id(), r.articleId(), names.getOrDefault(r.articleId(), "Subscription"), r.requestedAt(), r.status());
    }
}
