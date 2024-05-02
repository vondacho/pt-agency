package ch.obya.pta.booking.domain;

import java.util.Objects;
import java.util.UUID;

public record ArticleId(UUID id) {
    public ArticleId {
        Objects.requireNonNull(id);
    }
}
