package ch.obya.pta.www.shop;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;

import io.smallrye.mutiny.Uni;

/**
 * The articles sold by the agency, as held by pta-shopping.
 */
@RegisterRestClient(configKey = "shopping")
@Path("/articles")
public interface ShoppingClient {

    /** pta-shopping's representation, value objects included. */
    record Article(Ref id, Content name, Content description, Validity validity, Price price,
                   Set<Tag> profile, Set<Tag> grants, Quota quota, Course course) {

        public boolean hasProfile(String tag) {
            return profile != null && profile.contains(new Tag(tag));
        }
    }

    record Ref(UUID id) {
    }

    record Content(String content) {
    }

    record Validity(LocalDate from, LocalDate to) {
    }

    record Price(Double value) {
    }

    record Tag(String name) {
    }

    record Quota(Integer min, Integer max) {
    }

    /** What a course (session article) is about and how to take part. */
    record Course(List<String> objectives, String level, String prerequisites, String whatToBring, Integer cancellationHours) {
    }

    @GET
    @Path("/{id}")
    Uni<Article> article(@RestPath UUID id);

    /** The subscriptions and passes giving access to a session article. */
    @GET
    @Path("/{id}/subscriptions")
    Uni<List<Ref>> eligibleSubscriptions(@RestPath UUID id);

    /**
     * @param tag    profile tags, any of them (e.g. SUBSCRIPTION, PASS, SESSION)
     * @param onSale only the articles on sale today
     */
    @GET
    Uni<List<Article>> articles(@RestQuery List<String> tag, @RestQuery boolean onSale);
}
