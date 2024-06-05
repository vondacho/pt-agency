package ch.obya.pta.common.infrastructure.messaging;

import java.util.Collection;

import io.smallrye.mutiny.Uni;

import ch.obya.pta.common.application.EventPublisher;
import ch.obya.pta.common.domain.event.Event;

public class RdbmsEventPublisher implements EventPublisher {
    @Override
    public Uni<Void> publish(Collection<Event> events) {
        return null;
    }
}
