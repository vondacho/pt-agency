package ch.obya.pta.booking.domain;

import java.util.Objects;

public record Location(String name) {
    public Location {
        Objects.requireNonNull(name);
    }
}
