package ch.obya.pta.www.me;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.smallrye.mutiny.Uni;

import ch.obya.pta.www.booking.SessionView;
import ch.obya.pta.www.shop.ShoppingClient;
import ch.obya.pta.www.shop.ShoppingClient.Article;
import ch.obya.pta.www.shop.ShoppingClient.Content;
import ch.obya.pta.www.shop.ShoppingClient.Tag;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The participant's space and the catalogue, with pta-customer, pta-shopping and pta-booking mocked.
 */
@QuarkusTest
class MySpaceTest {

    static final String ALICE = "20000000-0000-0000-0000-00000000000a";
    static final UUID ALICE_ID = UUID.fromString(ALICE);
    static final UUID MONTHLY = UUID.randomUUID();
    static final UUID CARD = UUID.randomUUID();
    static final UUID SUMMER = UUID.randomUUID();
    static final LocalDate TODAY = LocalDate.now();

    @InjectMock
    @RestClient
    CustomerClient customers;

    @InjectMock
    @RestClient
    ShoppingClient shopping;

    @InjectMock
    @RestClient
    ParticipantBookingsClient bookings;

    private static Set<Tag> tags(String... names) {
        return java.util.Arrays.stream(names).map(Tag::new).collect(java.util.stream.Collectors.toSet());
    }

    private static Article article(UUID id, String name, double price, Set<Tag> profile, Set<Tag> grants, ShoppingClient.Quota quota) {
        return new Article(new ShoppingClient.Ref(id), new Content(name), new Content(name + " description"),
                new ShoppingClient.Validity(TODAY.minusYears(1), null), new ShoppingClient.Price(price), profile, grants, quota);
    }

    @BeforeEach
    void catalogue() {
        var monthly = article(MONTHLY, "Monthly group pass", 139, tags("SUBSCRIPTION", "ONE_MONTH"), tags("SESSION", "COURSE"), null);
        var card = article(CARD, "10-session group card", 220, tags("PASS", "ONE_YEAR"), tags("SESSION", "COURSE"), new ShoppingClient.Quota(0, 10));
        var summer = article(SUMMER, "Summer pass", 249, tags("SUBSCRIPTION", "THREE_MONTHS"), tags("SESSION", "COURSE"), null);
        var bootcamp = article(UUID.randomUUID(), "Bootcamp", 20, tags("SESSION", "COURSE"), Set.of(), null);
        var hiit = article(UUID.randomUUID(), "Small group hiit", 25, tags("SESSION", "COURSE"), Set.of(), null);
        var personal = article(UUID.randomUUID(), "Personal training", 90, tags("SESSION", "PERSONAL_TRAINING"), Set.of(), null);
        when(shopping.articles(eq(List.of("SUBSCRIPTION", "PASS")), eq(true))).thenReturn(Uni.createFrom().item(List.of(card, monthly)));
        when(shopping.articles(eq(List.of("SUBSCRIPTION", "PASS")), eq(false))).thenReturn(Uni.createFrom().item(List.of(card, monthly, summer)));
        when(shopping.articles(eq(List.of("SESSION")), anyBoolean())).thenReturn(Uni.createFrom().item(List.of(bootcamp, hiit, personal)));
    }

    @Test
    void catalogue_lists_subscriptions_then_passes_with_what_they_open() {
        given().when().get("/api/catalogue")
                .then().statusCode(200)
                .body("name", contains("Monthly group pass", "10-session group card"),
                        "[0].kind", equalTo("SUBSCRIPTION"),
                        "[0].duration", equalTo("1 month"),
                        "[0].sessions", nullValue(),
                        "[0].opens", contains("Bootcamp", "Small group hiit"),
                        "[1].kind", equalTo("PASS"),
                        "[1].duration", equalTo("1 year"),
                        "[1].sessions", equalTo(10));
    }

    @Test
    void unavailable_catalogue_answers_502() {
        when(shopping.articles(eq(List.of("SUBSCRIPTION", "PASS")), eq(true)))
                .thenReturn(Uni.createFrom().failure(new ProcessingException("Connection refused")));
        given().when().get("/api/catalogue").then().statusCode(502)
                .body("detail", equalTo("The catalogue is temporarily unavailable."));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "customer_id", value = ALICE))
    void subscriptions_are_current_or_coming_with_their_names_and_pending_requests() {
        var subscription = (java.util.function.BiFunction<UUID, CustomerClient.Validity, CustomerClient.Subscription>) (article, validity) ->
                new CustomerClient.Subscription(new CustomerClient.Ref(UUID.randomUUID()), new CustomerClient.Ref(article), validity, 0);
        when(customers.subscriptions(ALICE_ID)).thenReturn(Uni.createFrom().item(List.of(
                subscription.apply(SUMMER, new CustomerClient.Validity(TODAY.minusMonths(4), TODAY.minusMonths(1))),   // over
                subscription.apply(MONTHLY, new CustomerClient.Validity(TODAY.minusDays(10), TODAY.plusDays(20))),     // ends soon
                new CustomerClient.Subscription(new CustomerClient.Ref(UUID.randomUUID()), new CustomerClient.Ref(CARD),
                        new CustomerClient.Validity(TODAY.plusDays(5), TODAY.plusYears(1)), 10))));                    // coming
        when(customers.requests(ALICE_ID)).thenReturn(Uni.createFrom().item(List.of(
                new CustomerClient.SubscriptionRequest(UUID.randomUUID(), MONTHLY, Instant.now(), "PENDING", null),
                new CustomerClient.SubscriptionRequest(UUID.randomUUID(), CARD, Instant.now(), "CONFIRMED", UUID.randomUUID()))));

        given().when().get("/api/me/subscriptions")
                .then().statusCode(200)
                .body("subscriptions.name", contains("Monthly group pass", "10-session group card"),
                        "subscriptions[0].expiresSoon", equalTo(true),
                        "subscriptions[0].sessions", nullValue(),
                        "subscriptions[1].upcoming", equalTo(true),
                        "subscriptions[1].sessions", equalTo(10),
                        "requests.name", contains("Monthly group pass"));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "customer_id", value = ALICE))
    void a_subscription_of_the_catalogue_can_be_requested() {
        when(customers.request(eq(ALICE_ID), any())).thenReturn(Uni.createFrom().item(
                new CustomerClient.SubscriptionRequest(UUID.randomUUID(), MONTHLY, Instant.now(), "PENDING", null)));

        given().header("X-PTA-Request", "1").contentType("application/json").body("{\"articleId\":\"%s\"}".formatted(MONTHLY))
                .when().post("/api/me/subscription-requests")
                .then().statusCode(201)
                .body("name", equalTo("Monthly group pass"), "status", equalTo("PENDING"));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "customer_id", value = ALICE))
    void articles_outside_the_catalogue_cannot_be_requested() {
        given().header("X-PTA-Request", "1").contentType("application/json").body("{\"articleId\":\"%s\"}".formatted(SUMMER))
                .when().post("/api/me/subscription-requests")
                .then().statusCode(422)
                .body("detail", equalTo("This subscription cannot be requested."));
        verify(customers, never()).request(any(), any());
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "customer_id", value = ALICE))
    void a_second_request_is_explained() {
        when(customers.request(eq(ALICE_ID), any())).thenReturn(Uni.createFrom().failure(new WebApplicationException(409)));
        given().header("X-PTA-Request", "1").contentType("application/json").body("{\"articleId\":\"%s\"}".formatted(MONTHLY))
                .when().post("/api/me/subscription-requests")
                .then().statusCode(409)
                .body("detail", equalTo("You have already requested this subscription."));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "customer_id", value = ALICE))
    void a_handled_request_can_no_longer_be_cancelled() {
        var request = UUID.randomUUID();
        when(customers.cancel(ALICE_ID, request)).thenReturn(Uni.createFrom().failure(new WebApplicationException(422)));
        given().header("X-PTA-Request", "1")
                .when().delete("/api/me/subscription-requests/{id}", request)
                .then().statusCode(422)
                .body("detail", equalTo("This request has already been handled by the agency."));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "customer_id", value = ALICE))
    void requests_need_the_spa_header() {
        given().contentType("application/json").body("{\"articleId\":\"%s\"}".formatted(MONTHLY))
                .when().post("/api/me/subscription-requests")
                .then().statusCode(403);
        verify(customers, never()).request(any(), any());
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "customer_id", value = ALICE))
    void bookings_are_the_coming_ones_of_the_participant() {
        var session = new SessionView(UUID.randomUUID(), "Bootcamp", TODAY.plusDays(1), LocalTime.of(10, 0), LocalTime.of(11, 0),
                "City park", 3, 20, 4, 16, 0, true, "DONE");
        when(bookings.bookings(ALICE_ID, TODAY)).thenReturn(Uni.createFrom().item(List.of(session)));
        given().when().get("/api/me/bookings").then().statusCode(200)
                .body("title", contains("Bootcamp"), "participantBooking", contains("DONE"));
    }

    @Test
    void the_space_needs_a_signed_in_participant() {
        given().when().get("/api/me/subscriptions").then().statusCode(401);
        given().when().get("/api/me/bookings").then().statusCode(401);
        given().when().get("/api/me/profile").then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "carol")
    void accounts_not_linked_have_no_space() {
        given().when().get("/api/me/subscriptions").then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "customer_id", value = ALICE))
    void profile_is_read_from_the_customer() {
        when(customers.customer(ALICE_ID)).thenReturn(Uni.createFrom().item(new CustomerClient.Customer(
                new CustomerClient.Content("Alice"), new CustomerClient.Content("Martin"),
                new CustomerClient.Email("alice@example.ch"), new CustomerClient.Phone("+41791234567"),
                new CustomerClient.Address("Rue du Lac 12", "1003", "Lausanne", "VD", "CH"), null)));
        given().when().get("/api/me/profile").then().statusCode(200)
                .body("firstName", equalTo("Alice"), "email", equalTo("alice@example.ch"), "delivery.city", equalTo("Lausanne"));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "customer_id", value = ALICE))
    void contact_details_are_updated() {
        when(customers.reconnect(ALICE_ID, "alice@new.ch", "+41790000000")).thenReturn(Uni.createFrom().voidItem());
        given().header("X-PTA-Request", "1").contentType("application/json")
                .body("{\"email\":\" alice@new.ch \",\"phone\":\"+41790000000\"}")
                .when().put("/api/me/profile/contact")
                .then().statusCode(204);
        verify(customers).reconnect(ALICE_ID, "alice@new.ch", "+41790000000");
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "customer_id", value = ALICE))
    void invalid_contact_details_are_explained() {
        when(customers.reconnect(any(), any(), any())).thenReturn(Uni.createFrom().failure(new WebApplicationException(400)));
        given().header("X-PTA-Request", "1").contentType("application/json")
                .body("{\"email\":\"not-an-address\",\"phone\":\"123\"}")
                .when().put("/api/me/profile/contact")
                .then().statusCode(400)
                .body("detail", equalTo("Please check the e-mail address and the phone number."));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "customer_id", value = ALICE))
    void addresses_are_updated_in_the_customer_s_format() {
        when(customers.relocate(any(), any(), any())).thenReturn(Uni.createFrom().voidItem());
        given().header("X-PTA-Request", "1").contentType("application/json")
                .body("{\"delivery\":{\"streetNo\":\"Av. de Cour 1\",\"zip\":\"1007\",\"city\":\"Lausanne\",\"country\":\"CH\"}}")
                .when().put("/api/me/profile/address")
                .then().statusCode(204);
        verify(customers).relocate(ALICE_ID, "Av. de Cour 1,1007,Lausanne,,CH", null);
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "customer_id", value = ALICE))
    void incomplete_or_unsupported_addresses_are_refused() {
        given().header("X-PTA-Request", "1").contentType("application/json")
                .body("{\"delivery\":{\"streetNo\":\"Av. de Cour 1\",\"city\":\"Lausanne\",\"country\":\"CH\"}}")
                .when().put("/api/me/profile/address")
                .then().statusCode(400).body("detail", equalTo("Please give a postcode, a town and a country."));
        given().header("X-PTA-Request", "1").contentType("application/json")
                .body("{\"delivery\":{\"streetNo\":\"Av. de Cour 1, 2nd floor\",\"zip\":\"1007\",\"city\":\"Lausanne\",\"country\":\"CH\"}}")
                .when().put("/api/me/profile/address")
                .then().statusCode(400).body("detail", equalTo("Addresses cannot contain commas."));
        verify(customers, never()).relocate(any(), any(), any());
    }
}
