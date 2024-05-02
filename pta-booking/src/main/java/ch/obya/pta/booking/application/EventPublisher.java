package ch.obya.pta.booking.application;

import ch.obya.pta.booking.domain.DomainEvent;
import io.smallrye.mutiny.Uni;

import java.util.Collection;

public interface EventPublisher {

    Uni<Void> send(Collection<DomainEvent> events);
}
