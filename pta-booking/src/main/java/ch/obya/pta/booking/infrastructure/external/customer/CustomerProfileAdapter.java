package ch.obya.pta.booking.infrastructure.external.customer;

import java.time.LocalDate;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;

import io.smallrye.mutiny.Multi;

import ch.obya.pta.booking.application.CustomerProfile;
import ch.obya.pta.booking.domain.vo.ParticipantId;
import ch.obya.pta.booking.domain.vo.Subscription;

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
        Multi<SubscriptionDto> validSubscriptionsOf(@RestPath CustomerId id, @RestQuery LocalDate validAt);
    }
}
