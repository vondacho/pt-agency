package ch.obya.pta.common.infrastructure.web;

import ch.obya.pta.common.domain.vo.Name;
import ch.obya.pta.common.domain.vo.Quota;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;

@Provider
public class CommonAttributeConverterProvider implements ParamConverterProvider {

    private final Map<Class, ParamConverter> converters = Map.of(
            Name.class, new NameConverter(),
            Quota.class, new QuotaConverter()
    );

    @SuppressWarnings("unchecked")
    @Override
    public <T> ParamConverter<T> getConverter(Class<T> aClass, Type type, Annotation[] annotations) {
        return converters.getOrDefault(aClass, null);
    }

    private static class NameConverter implements ParamConverter<Name> {
        @Override
        public Name fromString(String value) {
            return new Name(value);
        }

        @Override
        public String toString(Name value) {
            return value.content();
        }
    }

    private static class QuotaConverter implements ParamConverter<Quota> {
        @Override
        public Quota fromString(String value) {
            var fields = value.split(",");
            return new Quota(Integer.valueOf(fields[0]), Integer.valueOf(fields[1]));
        }

        @Override
        public String toString(Quota value) {
            return "%d,%d".formatted(value.min(), value.max());
        }
    }
}
