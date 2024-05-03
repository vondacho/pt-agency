package ch.obya.pta.booking.domain;

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

import java.time.LocalDate;
import java.util.Objects;

public record Subscription(SubscriptionId id,
                           ArticleId articleId,
                           Validity validity,
                           Quota quota) {
    public record Validity(LocalDate from, LocalDate to) {
        public Validity {
            Objects.requireNonNull(from);
            if (to != null && from.isAfter(to))
                throw BookingProblem.SubscriptionValidityInvalid.toException(from, to);
        }

        public boolean includes(LocalDate date) {
            return !(date.isBefore(from) || (to == null || date.isAfter(to)));
        }
    }

    public Subscription {
        Objects.requireNonNull(id);
        Objects.requireNonNull(articleId);
        Objects.requireNonNull(validity);
    }
}
