package ch.obya.pta.www.booking;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

import ch.obya.pta.www.me.CustomerClient;
import ch.obya.pta.www.shop.ShoppingClient;
import ch.obya.pta.www.shop.ShoppingClient.Article;
import ch.obya.pta.www.shop.ShoppingClient.Content;
import ch.obya.pta.www.shop.ShoppingClient.Course;
import ch.obya.pta.www.shop.ShoppingClient.Ref;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@QuarkusTest
class SessionOverviewTest {

    static final String ALICE = "20000000-0000-0000-0000-00000000000a";
    static final UUID SESSION = UUID.randomUUID();
    static final UUID COURSE = UUID.randomUUID();
    static final UUID UNLIMITED = UUID.randomUUID();
    static final UUID TEN_SESSIONS = UUID.randomUUID();
    static final UUID SUMMER = UUID.randomUUID();
    static final LocalDate DAY = LocalDate.now().plusDays(3);

    @InjectMock
    @RestClient
    BookingClient booking;

    @InjectMock
    @RestClient
    ShoppingClient shopping;

    @InjectMock
    @RestClient
    CustomerClient customers;

    @BeforeEach
    void bootcamp() {
        var session = new SessionView(SESSION, "Bootcamp", DAY, LocalTime.of(10, 0), LocalTime.of(11, 0), "Parc de Milan",
                3, 20, 4, 16, 0, true, null, "Marco Rossi");
        var details = new BookingClient.SessionDetails(session, COURSE,
                new BookingClient.Instructor(UUID.randomUUID(), "Marco Rossi", "Group coach"),
                new BookingClient.Venue(UUID.randomUUID(), "Parc de Milan", "Avenue de Milan, 1007 Lausanne", "At the fountain", "https://map"),
                "Partner challenge", 12, LocalDateTime.of(DAY, LocalTime.of(10, 0)).minusHours(12));
        when(booking.details(eq(SESSION), any())).thenReturn(Uni.createFrom().item(details));
        when(shopping.article(COURSE)).thenReturn(Uni.createFrom().item(article(COURSE, "Bootcamp",
                new Course(List.of("Outdoor circuits", "Team spirit"), "All levels", "From 14 years", "Water and a mat", 12))));
        when(shopping.eligibleSubscriptions(COURSE)).thenReturn(Uni.createFrom().item(
                List.of(new Ref(UNLIMITED), new Ref(TEN_SESSIONS), new Ref(SUMMER))));
        when(shopping.articles(any(), eq(false))).thenReturn(Uni.createFrom().item(List.of(
                article(UNLIMITED, "Unlimited pass", null), article(TEN_SESSIONS, "10-session group card", null),
                article(SUMMER, "Summer pass", null))));
        when(shopping.articles(any(), eq(true))).thenReturn(Uni.createFrom().item(List.of(
                article(UNLIMITED, "Unlimited pass", null), article(TEN_SESSIONS, "10-session group card", null))));
    }

    private static Article article(UUID id, String name, Course course) {
        return new Article(new Ref(id), new Content(name), null, null, null, Set.of(), Set.of(), null, course);
    }

    @Test
    void anyone_gets_the_course_the_instructor_the_venue_and_the_conditions() {
        given().when().get("/api/sessions/{id}/overview", SESSION)
                .then().statusCode(200)
                .body("session.title", equalTo("Bootcamp"),
                        "course.objectives", contains("Outdoor circuits", "Team spirit"),
                        "course.level", equalTo("All levels"),
                        "instructor.name", equalTo("Marco Rossi"),
                        "venue.address", equalTo("Avenue de Milan, 1007 Lausanne"),
                        "note", equalTo("Partner challenge"),
                        "cancellationHours", equalTo(12),
                        "cancellationOpen", equalTo(true),
                        // the summer pass is not on sale: not offered to someone who does not hold it
                        "access.name", contains("10-session group card", "Unlimited pass"),
                        "access[0].onSale", equalTo(true),
                        "youHoldOne", nullValue(),
                        "incomplete", equalTo(false));
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "customer_id", value = ALICE))
    void a_participant_learns_which_subscription_they_hold() {
        var alice = UUID.fromString(ALICE);
        when(customers.subscriptions(alice)).thenReturn(Uni.createFrom().item(List.of(
                new CustomerClient.Subscription(new CustomerClient.Ref(UUID.randomUUID()), new CustomerClient.Ref(SUMMER),
                        new CustomerClient.Validity(DAY.minusMonths(1), null), null),
                // expired before the session: not held
                new CustomerClient.Subscription(new CustomerClient.Ref(UUID.randomUUID()), new CustomerClient.Ref(UNLIMITED),
                        new CustomerClient.Validity(DAY.minusYears(1), DAY.minusDays(1)), null))));

        given().when().get("/api/sessions/{id}/overview", SESSION)
                .then().statusCode(200)
                .body("access.name", contains("Summer pass", "10-session group card", "Unlimited pass"),
                        "access[0].held", equalTo(true),
                        "access[0].onSale", equalTo(false),
                        "access[2].held", equalTo(false),
                        "youHoldOne", equalTo(true));
    }

    @Test
    void without_the_catalogue_the_overview_is_incomplete_but_readable() {
        when(shopping.article(COURSE)).thenReturn(Uni.createFrom().failure(new ProcessingException("Connection refused")));
        when(shopping.eligibleSubscriptions(COURSE)).thenReturn(Uni.createFrom().failure(new ProcessingException("Connection refused")));

        given().when().get("/api/sessions/{id}/overview", SESSION)
                .then().statusCode(200)
                .body("venue.name", equalTo("Parc de Milan"),
                        "course", nullValue(),
                        "access.size()", equalTo(0),
                        "incomplete", equalTo(true));
    }

    @Test
    void an_unknown_session_is_404_and_an_unavailable_booking_502() {
        var unknown = UUID.randomUUID();
        when(booking.details(eq(unknown), any())).thenReturn(Uni.createFrom().failure(new WebApplicationException(404)));
        given().when().get("/api/sessions/{id}/overview", unknown).then().statusCode(404);

        when(booking.details(eq(unknown), any())).thenReturn(Uni.createFrom().failure(new ProcessingException("down")));
        given().when().get("/api/sessions/{id}/overview", unknown).then().statusCode(502);
    }

    @Test
    @TestSecurity(user = "alice")
    @OidcSecurity(claims = @Claim(key = "customer_id", value = ALICE))
    void a_refused_cancellation_tells_the_deadline() {
        when(booking.cancel(SESSION, UUID.fromString(ALICE))).thenReturn(Uni.createFrom().failure(new WebApplicationException(422)));

        given().header("X-PTA-Request", "1")
                .when().delete("/api/sessions/{id}/booking", SESSION)
                .then().statusCode(422)
                .body("detail", equalTo("Cancellations close 12 hours before the session."));
    }
}
