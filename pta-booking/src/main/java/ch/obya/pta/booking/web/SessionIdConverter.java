package ch.obya.pta.booking.web;

import ch.obya.pta.booking.domain.SessionId;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.UUID;

public class SessionIdConverter implements ParamConverter<SessionId> {

    @Override
    public SessionId fromString(String value) {
        return new SessionId(UUID.fromString(value));
    }

    @Override
    public String toString(SessionId value) {
        return value.id().toString();
    }

    @Provider
    public static class SessionIdConverterProvider implements ParamConverterProvider {
        @SuppressWarnings("unchecked")
        @Override
        public <T> ParamConverter<T> getConverter(Class<T> aClass, Type type, Annotation[] annotations) {
            return type == SessionId.class ? (ParamConverter<T>)new SessionIdConverter() : null;
        }
    }
}
