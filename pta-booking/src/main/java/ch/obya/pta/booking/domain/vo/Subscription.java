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

import ch.obya.pta.common.domain.vo.Validity;
import ch.obya.pta.common.domain.util.CommonProblem;
import lombok.Builder;

import static ch.obya.pta.common.domain.util.CommonProblem.ifNullThrow;

@Builder(builderClassName = "Builder", toBuilder = true)
public record Subscription(SubscriptionId id,
                           ArticleId articleId,
                           Validity validity) {

    public Subscription {
        ifNullThrow(id, CommonProblem.AttributeNotNull.toException("Subscription.id"));
        ifNullThrow(articleId, CommonProblem.AttributeNotNull.toException("Subscription.articleId"));
        ifNullThrow(validity, CommonProblem.AttributeNotNull.toException("Subscription.validity"));
    }
}
