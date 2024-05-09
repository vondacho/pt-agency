package ch.obya.pta.common.util.exception;

import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;

import java.util.function.Supplier;

public interface Problem {

    String name();

    String text();

    default Supplier<Exception> toException(Object...parameters) {
        return () -> new Exception(name(), text(), parameters);
    }

    @Getter
    @Accessors(fluent = true)
    @FieldDefaults(makeFinal=true)
    class Exception extends RuntimeException {
        private String code;
        private Object[] parameters;

        private Exception(String code, String template, Object...parameters) {
            super(template.formatted(parameters));
            this.code = code;
            this.parameters = parameters;
        }
    }
}
