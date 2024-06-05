package ch.obya.pta.booking.infrastructure.external.shopping;


import ch.obya.pta.booking.application.ArticleStore;
import ch.obya.pta.booking.domain.vo.ArticleId;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.RestPath;

@ApplicationScoped
public class ArticleStoreAdapter implements ArticleStore {

    @RestClient
    ArticleStoreRestClient client;

    @Override
    public Multi<ArticleId> eligibleSubscriptionsFor(ArticleId session) {
        return client.eligibleSubscriptions(session);
    }

    @Path("/articles")
    @RegisterRestClient
    private interface ArticleStoreRestClient {
        @GET
        @Path("/{id}/subscriptions")
        Multi<ArticleId> eligibleSubscriptions(@RestPath ArticleId id);
    }
}
