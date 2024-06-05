package ch.obya.pta.common.domain.util;

import java.util.Collection;
import java.util.function.Supplier;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

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
