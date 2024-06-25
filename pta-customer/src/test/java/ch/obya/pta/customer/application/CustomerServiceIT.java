package ch.obya.pta.customer.application;

import ch.obya.pta.common.application.EventPublisher;
import ch.obya.pta.customer.domain.util.Samples;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@QuarkusTest
public class CustomerServiceIT {

    @InjectMock
    EventPublisher eventPublisher;
    @Inject
    CustomerService customerService;

    @RunOnVertxContext
    @Test
    void create_customer_should_persist_and_publish_and_return_expected_id() {
        var example = Samples.oneCustomer.get().state();

        when(eventPublisher.publish(anyCollection())).thenReturn(Uni.createFrom().voidItem());

        var result = customerService.create(
                example.person(),
                example.deliveryAddress(),
                example.billingAddress(),
                example.emailAddress(),
                example.phoneNumber(),
                example.notes()).subscribe().withSubscriber(UniAssertSubscriber.create()).getItem();
    }
}
