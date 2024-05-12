package ch.obya.pta.customer.domain.util;

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

import ch.obya.pta.common.domain.util.Problem;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum CustomerProblem implements Problem {

    NoCustomer("Customer %s does not exist."),
    BirthDateInvalid("Birth date %s is not valid and cannot be in the future."),
    EmailAddressInvalid("Email address %s is not valid and should match %s."),
    PhoneNumberInvalid("Phone number %s is not valid and should match %s or %s."),
    SubscriptionWithoutCredit("Subscription %s does not have credits."),
    SubscriptionOverlapsExisting("Subscription %s overlaps existing subscription %s."),
    AlreadyExisting("Customer (%s,%s,%s,%s) already exists.");

    private final String text;
}
