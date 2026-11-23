package ch.obya.pta.customer.infrastructure.data;

import java.util.UUID;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;

import ch.obya.pta.customer.domain.aggregate.Customer;
import ch.obya.pta.customer.domain.entity.Subscription;
import ch.obya.pta.customer.domain.util.Samples;
import ch.obya.pta.customer.domain.vo.CustomerId;
import ch.obya.pta.customer.domain.vo.EmailAddress;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class PanacheCustomerRepositoryAdapterTest {

    static final CustomerId ALICE = new CustomerId(UUID.fromString("20000000-0000-0000-0000-00000000000a"));

    @Inject
    PanacheCustomerRepositoryAdapter repository;

    /** Samples share one e-mail address; make each saved customer distinct. */
    private static Customer distinct(Customer customer) {
        customer.modify().reconnect(new EmailAddress("john.%d@doe.ch".formatted(System.nanoTime())), null).done();
        return customer;
    }

    @RunOnVertxContext
    @Test
    void saved_customer_should_be_found_with_its_state_and_subscriptions(UniAsserter asserter) {
        var customer = distinct(Samples.oneCustomerWithOneYearSubscription.get());

        asserter.assertThat(
                () -> repository.save(customer).flatMap(repository::findOne),
                found -> {
                    assertThat(found.id()).isEqualTo(customer.id());
                    assertThat(found.state()).isEqualTo(customer.state());
                    assertThat(found.domainEvents()).isEmpty();
                    assertThat(found.subscriptions())
                            .extracting(Subscription::id, Subscription::articleId, s -> s.state())
                            .containsExactlyElementsOf(customer.subscriptions().stream()
                                    .map(s -> org.assertj.core.groups.Tuple.tuple(s.id(), s.articleId(), s.state()))
                                    .toList());
                });
    }

    @RunOnVertxContext
    @Test
    void updating_the_state_should_keep_the_subscriptions(UniAsserter asserter) {
        var customer = distinct(Samples.oneCustomerWithOneYearSubscription.get());

        asserter.execute(() -> repository.save(customer));
        asserter.execute(() -> customer.modify().annotate("updated notes").done());
        asserter.assertThat(
                () -> repository.save(customer.id(), customer.state()).flatMap(repository::findOne),
                found -> assertThat(found.subscriptions()).hasSize(1));
    }

    @RunOnVertxContext
    @Test
    void removed_customer_should_no_longer_be_found_with_its_subscriptions(UniAsserter asserter) {
        var customer = distinct(Samples.oneCustomerWithOneYearSubscription.get());

        asserter.execute(() -> repository.save(customer));
        asserter.execute(() -> repository.remove(customer.id()));
        asserter.assertThat(() -> repository.findOne(customer.id()), found -> assertThat(found).isNull());
    }

    @RunOnVertxContext
    @Test
    void seeded_customer_should_hold_an_unlimited_pass(UniAsserter asserter) {
        asserter.assertThat(
                () -> repository.findOne(ALICE),
                alice -> {
                    assertThat(alice.state().emailAddress().address()).isEqualTo("alice@example.ch");
                    assertThat(alice.subscriptions()).singleElement()
                            .satisfies(s -> assertThat(s.articleId().id()).hasToString("50000000-0000-0000-0000-000000000001"));
                });
    }

    @RunOnVertxContext
    @Test
    void unknown_customer_should_not_be_found(UniAsserter asserter) {
        asserter.assertThat(() -> repository.findOne(CustomerId.create()), found -> assertThat(found).isNull());
    }
}
