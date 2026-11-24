package ch.obya.pta.booking.application;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import ch.obya.pta.booking.domain.vo.ArticleId;

public interface ArticleStore {

    Multi<ArticleId> eligibleSubscriptionsFor(ArticleId session);

    /**
     * Hours before the start until which a booked seat can be cancelled, as set on the course; a null item when the
     * course has no deadline. Fails with {@code BookingProblem.ServiceUnavailable} when pta-shopping cannot answer.
     */
    Uni<Integer> cancellationHoursOf(ArticleId article);
}
