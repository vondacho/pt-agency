package ch.obya.pta.booking.infrastructure.external.shopping;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.RestPath;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import ch.obya.pta.booking.application.ArticleStore;
import ch.obya.pta.booking.domain.util.BookingProblem;
import ch.obya.pta.booking.domain.vo.ArticleId;

/**
 * Subscription articles giving access to a session's article, as defined by pta-shopping.
 */
@ApplicationScoped
public class ArticleStoreAdapter implements ArticleStore {

    @RestClient
    ArticleStoreRestClient client;

    @Override
    public Multi<ArticleId> eligibleSubscriptionsFor(ArticleId article) {
        return client.eligibleSubscriptions(article.id())
                .onFailure().transform(f -> BookingProblem.ServiceUnavailable.toException("pta-shopping", f.getMessage()).get())
                .onItem().transformToMulti(list -> Multi.createFrom().iterable(list));
    }

    @Path("/articles")
    @RegisterRestClient(configKey = "shopping")
    public interface ArticleStoreRestClient {
        @GET
        @Path("/{id}/subscriptions")
        Uni<List<ArticleId>> eligibleSubscriptions(@RestPath UUID id);
    }
}
