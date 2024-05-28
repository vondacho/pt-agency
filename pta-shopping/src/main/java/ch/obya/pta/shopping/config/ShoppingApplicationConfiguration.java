package ch.obya.pta.shopping.config;

import ch.obya.pta.common.application.EventPublisher;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

public class ShoppingApplicationConfiguration {
    @ApplicationScoped
    EventPublisher eventPublisher() {
        return events -> Uni.createFrom().voidItem();
    }
}
