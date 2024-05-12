package ch.obya.pta.common.domain.util;

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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Collection;
import java.util.function.Supplier;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum CommonProblem implements Problem {

    AttributeNotNull("Attribute %s cannot be null."),
    AttributeNotEmpty("Attribute %s cannot be empty."),
    ValueNotNull("Value %s cannot be null."),
    ValueNotEmpty("Value %s cannot be empty."),
    EntityNotFound("%s %s does not exist."),
    QuotaInvalid("Quota (%s, %s) is not valid."),
    ValidityInvalid("Validity (%s, %s) is not valid.");

    private final String text;

    public static void ifThrow(Supplier<Boolean> trigger, Supplier<Exception> exceptionSupplier) {
        if (trigger.get()) throw exceptionSupplier.get();
    }

    public static void ifNullThrow(Object o, Supplier<Exception> exceptionSupplier) {
        ifThrow(() -> o == null, exceptionSupplier);
    }

    public static void ifEmptyThrow(String s, Supplier<Exception> exceptionSupplier) {
        ifThrow(s::isEmpty, exceptionSupplier);
    }

    public static void ifEmptyThrow(Collection<?> collection, Supplier<Exception> exceptionSupplier) {
        ifThrow(collection::isEmpty, exceptionSupplier);
    }
}
