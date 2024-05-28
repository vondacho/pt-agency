package ch.obya.pta.booking.config;

import ch.obya.pta.common.application.EventPublisher;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

public class BookingApplicationConfiguration {
    @ApplicationScoped
    EventPublisher eventPublisher() {
        return events -> Uni.createFrom().voidItem();
    }
}
