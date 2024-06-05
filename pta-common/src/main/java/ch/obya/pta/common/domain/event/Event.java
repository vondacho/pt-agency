package ch.obya.pta.common.domain.event;

import java.time.Instant;

public interface Event {
    Instant timestamp();
}
