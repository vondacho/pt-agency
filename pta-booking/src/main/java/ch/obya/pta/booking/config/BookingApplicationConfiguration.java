package ch.obya.pta.booking.config;

import jakarta.enterprise.context.ApplicationScoped;

import io.smallrye.mutiny.Uni;

import ch.obya.pta.common.application.EventPublisher;

public class BookingApplicationConfiguration {
    @ApplicationScoped
    EventPublisher eventPublisher() {
        return events -> Uni.createFrom().voidItem();
    }
}
