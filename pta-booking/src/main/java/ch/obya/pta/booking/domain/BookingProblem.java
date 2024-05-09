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

import ch.obya.pta.common.util.exception.Problem;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum BookingProblem implements Problem {

    NoSession("Session %s does not exist."),
    SessionQuotaReached("Session %s: no more participant allowed, the quota (%d) is reached."),
    SubscriptionQuotaReached("Subscription %s: no more session allowed, the quota (%d) is reached."),
    NoActiveSubscription("No active subscription of participant %s applies to session %s (article %s)."),
    SubscriptionStillNotActive("Subscription %s is still not active (%s < %s)"),
    SubscriptionNoMoreActive("Subscription %s is no more active (%s > %s)."),
    SubscriptionValidityInvalid("Subscription validity (%s, %s) is not valid."),
    NoBooking("Booking on session %s for participant %s does not exist."),
    NoSessionForBooking("Session with booking %s does not exist."),
    QuotaInvalid("Quota (%s, %s) is not valid."),
    TimeSlotInvalid("Week time slot (%s, %s, %s, %s) is not valid.");

    private final String text;
}
