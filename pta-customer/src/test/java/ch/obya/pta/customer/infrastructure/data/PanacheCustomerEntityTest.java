package ch.obya.pta.customer.infrastructure.data;

import io.quarkus.test.hibernate.reactive.panache.TransactionalUniAsserter;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import org.junit.jupiter.api.Test;

import java.util.UUID;

@QuarkusTest
public class PanacheCustomerEntityTest {

    @RunOnVertxContext
    @Test
    void should_find_customer(TransactionalUniAsserter asserter) {
        asserter.execute(() -> PanacheCustomerEntity
                .find("logicalId", UUID.fromString("f1b9b1b1-1b1b-1b1b-1b1b-1b1b1b1b1b1b"))
                .firstResult());
    }

    @RunOnVertxContext
    @Test
    void should_find_customers(TransactionalUniAsserter asserter) {
        asserter.execute(() -> PanacheCustomerEntity
                .find("from customer where lastName = 'Doe' and birth = {d '1966-04-05'} and email = 'john@doe.ch' and firstName = 'John'")
                .list());
    }
}
