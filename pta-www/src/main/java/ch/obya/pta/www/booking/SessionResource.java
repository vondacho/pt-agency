package ch.obya.pta.www.booking;

import java.time.LocalDate;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.WebApplicationException;

import ch.obya.pta.www.me.CustomerClient;
import ch.obya.pta.www.shop.ShoppingClient;

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

    @RestClient
    ShoppingClient shopping;

    @RestClient
    CustomerClient customers;

    /**
     * Sessions of the period; for a signed-in participant, each one tells the status of their booking.
     */
    @GET
    public Uni<List<SessionView>> list(@RestQuery LocalDate from, @RestQuery LocalDate to) {
        return booking.sessions(from, to, participant.id().orElse(null))
                .onFailure().transform(SessionResource::toClientError);
    }

    /**
     * A session's overview: course objectives, instructor, venue and the conditions to take part. Public; the
     * signed-in participant also learns their booking and whether they hold a subscription giving access.
     * When the catalogue or the customer's subscriptions cannot be fetched, the rest is answered, flagged incomplete.
     */
    @GET
    @Path("/{id}/overview")
    public Uni<SessionOverview> overview(@RestPath UUID id) {
        var linked = participant.id().orElse(null);
        return booking.details(id, linked)
                .onFailure().transform(SessionResource::toClientError)
                .flatMap(details -> {
                    Uni<Optional<ShoppingClient.Article>> course = shopping.article(details.articleId())
                            .map(Optional::of).onFailure().recoverWithItem(Optional.empty());
                    Uni<Optional<List<SessionOverview.Access>>> access = access(details, linked)
                            .map(Optional::of).onFailure().recoverWithItem(Optional.empty());
                    return Uni.combine().all().unis(course, access).asTuple().map(parts -> {
                        var article = parts.getItem1();
                        var accesses = parts.getItem2();
                        var deadline = details.cancellableUntil();
                        return new SessionOverview(
                                details.session(),
                                article.map(a -> a.course()).map(c -> new SessionOverview.Course(
                                        c.objectives(), c.level(), c.prerequisites(), c.whatToBring())).orElse(null),
                                details.instructor(),
                                details.venue(),
                                details.note(),
                                details.cancellationHours(),
                                deadline,
                                deadline == null || LocalDateTime.now().isBefore(deadline),
                                accesses.orElse(List.of()),
                                linked == null ? null : accesses.map(l -> l.stream().anyMatch(SessionOverview.Access::held)).orElse(null),
                                article.isEmpty() || accesses.isEmpty());
                    });
                });
    }

    /** Subscriptions and passes opening the session: those on sale, and those the participant holds on its day. */
    private Uni<List<SessionOverview.Access>> access(BookingClient.SessionDetails details, UUID linked) {
        var day = details.session().day();
        Uni<Set<UUID>> held = linked == null ? Uni.createFrom().item(Set.of()) : customers.subscriptions(linked)
                .map(subscriptions -> subscriptions.stream()
                        .filter(s -> s.validity() == null
                                || (!day.isBefore(s.validity().from()) && (s.validity().to() == null || !day.isAfter(s.validity().to()))))
                        .map(s -> s.articleId().id())
                        .collect(Collectors.toSet()));
        return Uni.combine().all().unis(
                        shopping.eligibleSubscriptions(details.articleId()),
                        shopping.articles(List.of("SUBSCRIPTION", "PASS"), false),
                        shopping.articles(List.of("SUBSCRIPTION", "PASS"), true),
                        held)
                .with((eligible, all, onSale, holds) -> {
                    var saleIds = onSale.stream().map(a -> a.id().id()).collect(Collectors.toSet());
                    return all.stream()
                            .filter(a -> eligible.stream().anyMatch(e -> e.id().equals(a.id().id())))
                            .map(a -> new SessionOverview.Access(a.id().id(), a.name().content(),
                                    saleIds.contains(a.id().id()), holds.contains(a.id().id())))
                            .filter(a -> a.onSale() || a.held())
                            .sorted(Comparator.comparing(SessionOverview.Access::held).reversed()
                                    .thenComparing(SessionOverview.Access::name))
                            .toList();
                });
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
        var linked = participant.requireLinked(requestHeader);
        return booking.cancel(id, linked)
                .onFailure(SessionResource::isUnprocessable).recoverWithUni(failure -> cancellationClosed(id, linked))
                .onFailure().transform(SessionResource::toClientError);
    }

    /** Booking refuses a cancellation (422) only once the course's deadline has passed: tell which one. */
    private Uni<Void> cancellationClosed(UUID id, UUID linked) {
        return booking.details(id, linked)
                .map(BookingClient.SessionDetails::cancellationHours)
                .onFailure().recoverWithNull()
                .flatMap(hours -> Uni.createFrom().failure(Problems.problem(422, hours == null
                        ? "Cancellations are closed for this session."
                        : "Cancellations close %d hours before the session.".formatted(hours))));
    }

    private static boolean isUnprocessable(Throwable failure) {
        return failure instanceof WebApplicationException wae && wae.getResponse().getStatus() == 422
                && !(wae.getResponse().getEntity() instanceof java.util.Map<?, ?>);
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
