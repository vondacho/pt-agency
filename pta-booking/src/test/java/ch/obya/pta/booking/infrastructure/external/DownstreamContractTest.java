package ch.obya.pta.booking.infrastructure.external;

import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.client.WireMock;

import io.quarkiverse.wiremock.devservice.ConnectWireMock;
import io.quarkus.test.junit.QuarkusTest;

import ch.obya.pta.booking.domain.vo.ArticleId;
import ch.obya.pta.booking.domain.vo.ParticipantId;
import ch.obya.pta.booking.infrastructure.external.customer.CustomerProfileAdapter;
import ch.obya.pta.booking.infrastructure.external.shopping.ArticleStoreAdapter;
import ch.obya.pta.common.domain.util.Problem;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The HTTP contracts pta-booking relies on, against a WireMock server standing in for pta-customer and pta-shopping.
 * The JSON bodies are those the real services produce.
 */
@QuarkusTest
@ConnectWireMock
class DownstreamContractTest {

    static final Duration TIMEOUT = Duration.ofSeconds(10);

    WireMock wiremock;

    @Inject
    CustomerProfileAdapter customers;

    @Inject
    ArticleStoreAdapter articles;

    @BeforeEach
    void reset() {
        wiremock.resetMappings();
    }

    @Test
    void subscriptions_are_requested_for_the_session_day_and_mapped() {
        var participant = UUID.randomUUID();
        var day = LocalDate.of(2026, 12, 3);
        wiremock.register(get(urlPathEqualTo("/customers/%s/subscriptions".formatted(participant)))
                .withQueryParam("at", equalTo("2026-12-03"))
                .willReturn(okJson("""
                        [{"id":{"id":"40000000-0000-0000-0000-000000000001"},
                          "articleId":{"id":"50000000-0000-0000-0000-000000000001"},
                          "validity":{"from":"2026-11-01","to":"2027-10-31"},
                          "credits":0}]""")));

        var subscriptions = customers.validSubscriptionsOf(new ParticipantId(participant), day)
                .collect().asList().await().atMost(TIMEOUT);

        assertThat(subscriptions).singleElement().satisfies(s -> {
            assertThat(s.id().id()).hasToString("40000000-0000-0000-0000-000000000001");
            assertThat(s.articleId().id()).hasToString("50000000-0000-0000-0000-000000000001");
            assertThat(s.validity().to()).isEqualTo(LocalDate.of(2027, 10, 31));
        });
    }

    @Test
    void unknown_participant_holds_no_subscription() {
        var unknown = UUID.randomUUID();
        wiremock.register(get(urlPathEqualTo("/customers/%s/subscriptions".formatted(unknown)))
                .willReturn(aResponse().withStatus(404).withHeader("Content-Type", "application/problem+json")
                        .withBody("{\"status\":404,\"detail\":\"Customer does not exist.\"}")));

        assertThat(customers.validSubscriptionsOf(new ParticipantId(unknown), LocalDate.now())
                .collect().asList().await().atMost(TIMEOUT)).isEmpty();
    }

    @Test
    void failing_customer_service_is_not_reported_as_no_subscription() {
        var participant = UUID.randomUUID();
        wiremock.register(get(urlPathEqualTo("/customers/%s/subscriptions".formatted(participant)))
                .willReturn(aResponse().withStatus(503)));

        assertThatThrownBy(() -> customers.validSubscriptionsOf(new ParticipantId(participant), LocalDate.now())
                .collect().asList().await().atMost(TIMEOUT))
                .isInstanceOf(Problem.Exception.class)
                .satisfies(e -> assertThat(((Problem.Exception) e).code()).isEqualTo("ServiceUnavailable"));
    }

    @Test
    void eligible_subscription_articles_are_requested_and_mapped() {
        var session = UUID.randomUUID();
        wiremock.register(get(urlPathEqualTo("/articles/%s/subscriptions".formatted(session)))
                .willReturn(okJson("""
                        [{"id":"50000000-0000-0000-0000-000000000001"},{"id":"50000000-0000-0000-0000-000000000002"}]""")));

        var eligible = articles.eligibleSubscriptionsFor(new ArticleId(session))
                .collect().asList().await().atMost(TIMEOUT);

        assertThat(eligible).extracting(a -> a.id().toString())
                .containsExactly("50000000-0000-0000-0000-000000000001", "50000000-0000-0000-0000-000000000002");
    }

    @Test
    void the_course_cancellation_deadline_is_read_from_the_article() {
        var course = UUID.randomUUID();
        wiremock.register(get(urlPathEqualTo("/articles/%s".formatted(course))).willReturn(okJson("""
                {"id":{"id":"%s"},"name":{"content":"Bootcamp"},"course":{"objectives":["Cardio"],"cancellationHours":12}}""".formatted(course))));
        var none = UUID.randomUUID();
        wiremock.register(get(urlPathEqualTo("/articles/%s".formatted(none))).willReturn(okJson("""
                {"id":{"id":"%s"},"name":{"content":"Open gym"}}""".formatted(none))));

        assertThat(articles.cancellationHoursOf(new ArticleId(course)).await().atMost(TIMEOUT)).isEqualTo(12);
        assertThat(articles.cancellationHoursOf(new ArticleId(none)).await().atMost(TIMEOUT)).isNull();
    }
}
