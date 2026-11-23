package ch.obya.pta.booking.infrastructure.external.customer;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import ch.obya.pta.booking.application.CustomerProfile;
import ch.obya.pta.booking.domain.util.BookingProblem;
import ch.obya.pta.booking.domain.vo.ParticipantId;
import ch.obya.pta.booking.domain.vo.Subscription;

/**
 * Subscriptions of a participant, as held by pta-customer (a participant is a customer).
 */
@ApplicationScoped
public class CustomerProfileAdapter implements CustomerProfile {

    @RestClient
    CustomerRestClient client;

    /**
     * An unknown participant holds no subscription. Any other failure (pta-customer down, server error)
     * is reported as {@link BookingProblem#ServiceUnavailable}: it must not be mistaken for "no subscription".
     */
    @Override
    public Multi<Subscription> validSubscriptionsOf(ParticipantId participant, LocalDate validAt) {
        return client.validSubscriptionsOf(participant.id(), validAt)
                .onFailure(f -> f instanceof WebApplicationException wae && wae.getResponse().getStatus() == 404)
                .recoverWithItem(List.of())
                .onFailure().transform(f -> BookingProblem.ServiceUnavailable.toException("pta-customer", f.getMessage()).get())
                .onItem().transformToMulti(list -> Multi.createFrom().iterable(list))
                .map(dto -> new Subscription(dto.id(), dto.articleId(), dto.validity()));
    }

    @Path("/customers")
    @RegisterRestClient(configKey = "customer")
    public interface CustomerRestClient {
        @GET
        @Path("/{id}/subscriptions")
        Uni<List<SubscriptionDto>> validSubscriptionsOf(@RestPath UUID id, @RestQuery("at") LocalDate at);
    }
}
