package ch.obya.pta.customer.domain;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import ch.obya.pta.common.domain.util.Problem;
import ch.obya.pta.common.domain.vo.Validity;
import ch.obya.pta.customer.domain.entity.Subscription;
import ch.obya.pta.customer.domain.entity.SubscriptionRequest;
import ch.obya.pta.customer.domain.util.Samples;
import ch.obya.pta.customer.domain.vo.ArticleId;
import ch.obya.pta.customer.domain.vo.SubscriptionRequestId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubscriptionRequestTest {

    final ArticleId monthlyPass = ArticleId.create();
    final LocalDate today = LocalDate.now();

    private Subscription monthOf(ArticleId article, LocalDate from) {
        return new Subscription(new Validity(from, from.plusMonths(1).minusDays(1)), null, 0, article);
    }

    private static String code(Throwable e) {
        return ((Problem.Exception) e).code();
    }

    @Test
    void a_request_is_pending_until_confirmed() {
        var customer = Samples.oneCustomer.get();

        var request = customer.request(monthlyPass);

        assertThat(request.status()).isEqualTo(SubscriptionRequest.Status.PENDING);
        assertThat(customer.requests()).containsExactly(request);
        assertThat(customer.subscriptions()).isEmpty();
    }

    @Test
    void a_second_pending_request_for_the_same_article_is_refused() {
        var customer = Samples.oneCustomer.get();
        customer.request(monthlyPass);

        assertThatThrownBy(() -> customer.request(monthlyPass))
                .satisfies(e -> assertThat(code(e)).isEqualTo("RequestAlreadyPending"));
        // another article can be requested meanwhile
        assertThat(customer.request(ArticleId.create()).isPending()).isTrue();
    }

    @Test
    void confirming_creates_the_subscription() {
        var customer = Samples.oneCustomer.get();
        var request = customer.request(monthlyPass);
        var subscription = monthOf(monthlyPass, today);

        var confirmed = customer.confirm(request.id(), subscription);

        assertThat(confirmed.status()).isEqualTo(SubscriptionRequest.Status.CONFIRMED);
        assertThat(confirmed.subscription()).isEqualTo(subscription.id());
        assertThat(customer.subscriptions()).containsExactly(subscription);
        // the article can be requested again, e.g. to renew
        assertThat(customer.request(monthlyPass).isPending()).isTrue();
    }

    @Test
    void confirming_with_an_overlapping_subscription_is_refused_and_keeps_the_request_pending() {
        var customer = Samples.oneCustomer.get();
        customer.add(monthOf(monthlyPass, today));
        var request = customer.request(monthlyPass);

        assertThatThrownBy(() -> customer.confirm(request.id(), monthOf(monthlyPass, today.plusDays(10))))
                .satisfies(e -> assertThat(code(e)).isEqualTo("SubscriptionOverlapsExisting"));
        assertThat(customer.requestAt(request.id()).isPending()).isTrue();
        assertThat(customer.subscriptions()).hasSize(1);
    }

    @Test
    void confirming_with_another_article_is_refused() {
        var customer = Samples.oneCustomer.get();
        var request = customer.request(monthlyPass);

        assertThatThrownBy(() -> customer.confirm(request.id(), monthOf(ArticleId.create(), today)))
                .satisfies(e -> assertThat(code(e)).isEqualTo("RequestOfOtherArticle"));
    }

    @Test
    void only_pending_requests_can_be_cancelled_or_confirmed() {
        var customer = Samples.oneCustomer.get();
        var request = customer.request(monthlyPass);

        assertThat(customer.cancelRequest(request.id()).status()).isEqualTo(SubscriptionRequest.Status.CANCELLED);
        assertThatThrownBy(() -> customer.cancelRequest(request.id()))
                .satisfies(e -> assertThat(code(e)).isEqualTo("RequestNotPending"));
        assertThatThrownBy(() -> customer.confirm(request.id(), monthOf(monthlyPass, today)))
                .satisfies(e -> assertThat(code(e)).isEqualTo("RequestNotPending"));
    }

    @Test
    void unknown_requests_are_not_found() {
        var customer = Samples.oneCustomer.get();
        assertThatThrownBy(() -> customer.cancelRequest(SubscriptionRequestId.create()))
                .satisfies(e -> assertThat(code(e)).isEqualTo("EntityNotFound"));
    }
}
