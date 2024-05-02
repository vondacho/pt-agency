package ch.obya.pta.booking.eventing;

import ch.obya.pta.booking.application.EventPublisher;
import ch.obya.pta.booking.domain.DomainEvent;
import io.quarkus.arc.Unremovable;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Collection;

@Unremovable
@ApplicationScoped
public class DefaultEventPublisher implements EventPublisher {
    @Override
    public Uni<Void> send(Collection<DomainEvent> events) {
        return null;
    }
}
