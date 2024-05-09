package ch.obya.pta.booking.domain.vo;

/*-
 * #%L
 * pta-booking
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2024 obya
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import ch.obya.pta.booking.domain.BookingProblem;
import ch.obya.pta.common.util.exception.CommonProblem;
import lombok.Builder;

import java.time.LocalDate;

import static ch.obya.pta.common.util.exception.CommonProblem.ifNullThrow;
import static ch.obya.pta.common.util.exception.CommonProblem.ifThrow;

@Builder(builderClassName = "Builder", toBuilder = true)
public record Subscription(SubscriptionId id,
                           ArticleId articleId,
                           Validity validity,
                           Quota quota) {
    public record Validity(LocalDate from, LocalDate to) {
        public Validity {
            ifNullThrow(from, CommonProblem.AttributeNotNull.toException("Subscription.Validity.id"));
            ifThrow(() -> to != null && from.isAfter(to), BookingProblem.SubscriptionValidityInvalid.toException(from, to));
        }

        public boolean includes(LocalDate date) {
            return !(date.isBefore(from) || (to == null || date.isAfter(to)));
        }
    }

    public Subscription {
        ifNullThrow(id, CommonProblem.AttributeNotNull.toException("Subscription.id"));
        ifNullThrow(articleId, CommonProblem.AttributeNotNull.toException("Subscription.articleId"));
        ifNullThrow(validity, CommonProblem.AttributeNotNull.toException("Subscription.validity"));
    }
}
