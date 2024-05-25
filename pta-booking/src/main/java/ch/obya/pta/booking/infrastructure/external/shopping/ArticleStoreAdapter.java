package ch.obya.pta.booking.infrastructure.external.shopping;

/*-
 * #%L
 * pta-booking
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2024 obya
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import ch.obya.pta.booking.application.ArticleStore;
import ch.obya.pta.booking.domain.vo.ArticleId;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.RestQuery;

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
        Multi<ArticleId> eligibleSubscriptions(ArticleId id);
    }
}
