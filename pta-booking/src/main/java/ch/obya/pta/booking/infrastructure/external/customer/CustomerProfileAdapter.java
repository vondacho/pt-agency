package ch.obya.pta.booking.infrastructure.external.customer;

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

import ch.obya.pta.booking.application.CustomerProfile;
import ch.obya.pta.booking.domain.vo.ParticipantId;
import ch.obya.pta.booking.domain.vo.Subscription;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.RestQuery;

import java.time.LocalDate;

@ApplicationScoped
public class CustomerProfileAdapter implements CustomerProfile {

    @RestClient
    CustomerRestClient client;

    @Override
    public Multi<Subscription> validSubscriptionsOf(ParticipantId participant, LocalDate validAt) {
        return client.validSubscriptionsOf(new CustomerId(participant.id()), validAt)
                .map(dto -> new Subscription(dto.id(), dto.articleId(), dto.validity()));
    }

    @Path("/customers")
    @RegisterRestClient
    private interface CustomerRestClient {
        @GET
        @Path("/{id}/subscriptions")
        Multi<SubscriptionDto> validSubscriptionsOf(CustomerId id, @RestQuery LocalDate validAt);
    }
}
