package ch.obya.pta.www.shop;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.smallrye.mutiny.Uni;

import ch.obya.pta.www.shop.ShoppingClient.Article;
import ch.obya.pta.www.shop.ShoppingClient.Tag;

/**
 * The subscriptions and passes on sale, shaped for participants.
 */
@ApplicationScoped
public class Catalogue {

    /**
     * @param kind     SUBSCRIPTION (unlimited for a period) or PASS (a number of sessions)
     * @param duration e.g. "1 month", "1 year"
     * @param sessions sessions included, absent when unlimited
     * @param opens    the sessions it gives access to, e.g. "Bootcamp", "Personal training"
     */
    public record Item(UUID id, String kind, String name, String description, Double price,
                       String duration, Integer sessions, List<String> opens) {
    }

    private static final Map<String, String> DURATIONS = Map.of(
            "ONE_YEAR", "1 year", "THREE_MONTHS", "3 months", "TWO_MONTHS", "2 months",
            "ONE_MONTH", "1 month", "ONE_WEEK", "1 week", "ONE_DAY", "1 day", "ONE_SESSION", "1 session");
    /** What pta-customer applies to an article without a duration tag. */
    private static final String DEFAULT_DURATION = "1 year";
    private static final List<String> KINDS = List.of("SUBSCRIPTION", "PASS");

    @RestClient
    ShoppingClient shopping;

    /** Subscriptions first, then passes; cheapest first within each kind. */
    public Uni<List<Item>> items() {
        return shopping.articles(KINDS, true)
                .flatMap(offers -> shopping.articles(List.of("SESSION"), true).map(sessions ->
                        offers.stream()
                                .map(offer -> toItem(offer, sessions))
                                .sorted(Comparator.comparing((Item i) -> KINDS.indexOf(i.kind()))
                                        .thenComparing(i -> i.price() == null ? 0 : i.price()))
                                .toList()));
    }

    public Uni<Optional<Item>> item(UUID id) {
        return items().map(items -> items.stream().filter(i -> i.id().equals(id)).findFirst());
    }

    private static Item toItem(Article offer, List<Article> sessions) {
        var kind = offer.hasProfile("SUBSCRIPTION") ? "SUBSCRIPTION" : "PASS";
        var duration = offer.profile().stream()
                .map(Tag::name).map(DURATIONS::get)
                .filter(java.util.Objects::nonNull)
                .findFirst().orElse(DEFAULT_DURATION);
        var included = "PASS".equals(kind) && offer.quota() != null ? offer.quota().max() : null;
        // the same rule as booking: an offer opens a session when its grants cover the session's profile
        var opens = sessions.stream()
                .filter(s -> offer.grants() != null && offer.grants().containsAll(s.profile()))
                .map(s -> s.name().content())
                .distinct()
                .sorted()
                .toList();
        return new Item(offer.id().id(), kind, offer.name().content(),
                offer.description() == null ? null : offer.description().content(),
                offer.price() == null ? null : offer.price().value(),
                duration, included, opens);
    }
}
