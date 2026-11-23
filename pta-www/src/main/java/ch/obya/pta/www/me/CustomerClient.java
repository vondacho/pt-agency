package ch.obya.pta.www.me;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;

import io.smallrye.mutiny.Uni;

/**
 * Participants (customers), their subscriptions and subscription requests, as held by pta-customer.
 */
@RegisterRestClient(configKey = "customer")
@Path("/customers/{id}")
public interface CustomerClient {

    /** pta-customer's representation, value objects included. */
    record Customer(Content firstName, Content lastName, Email email, Phone phone, Address delivery, Address billing) {
    }

    record Content(String content) {
    }

    record Email(String address) {
    }

    record Phone(String number) {
    }

    record Address(String streetNo, String zip, String city, String region, String country) {
        /** pta-customer's query parameter format: `street,zip,city,region,country`. */
        public String asParameter() {
            return String.join(",", nonNull(streetNo), nonNull(zip), nonNull(city), nonNull(region), nonNull(country));
        }

        private static String nonNull(String value) {
            return value == null ? "" : value;
        }
    }

    record Subscription(Ref id, Ref articleId, Validity validity, Integer credits) {
    }

    record Ref(UUID id) {
    }

    record Validity(LocalDate from, LocalDate to) {
    }

    record SubscriptionRequest(UUID id, UUID articleId, Instant requestedAt, String status, UUID subscription) {
    }

    record NewRequest(UUID articleId) {
    }

    @GET
    Uni<Customer> customer(@RestPath UUID id);

    /** All the customer's subscriptions, past ones included. */
    @GET
    @Path("/subscriptions")
    Uni<List<Subscription>> subscriptions(@RestPath UUID id);

    @PATCH
    @Path("/connection")
    Uni<Void> reconnect(@RestPath UUID id, @RestQuery String email, @RestQuery String phone);

    @PATCH
    @Path("/location")
    Uni<Void> relocate(@RestPath UUID id, @RestQuery String delivery, @RestQuery String billing);

    @GET
    @Path("/subscription-requests")
    Uni<List<SubscriptionRequest>> requests(@RestPath UUID id);

    @POST
    @Path("/subscription-requests")
    Uni<SubscriptionRequest> request(@RestPath UUID id, NewRequest request);

    @DELETE
    @Path("/subscription-requests/{request}")
    Uni<SubscriptionRequest> cancel(@RestPath UUID id, @RestPath UUID request);
}
