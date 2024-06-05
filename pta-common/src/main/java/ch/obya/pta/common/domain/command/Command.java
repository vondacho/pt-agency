package ch.obya.pta.common.domain.command;

import java.time.Instant;

public interface Command {
    Instant timestamp();
}
