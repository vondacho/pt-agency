package ch.obya.pta.booking.infrastructure.web;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.UUID;

import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

import ch.obya.pta.booking.domain.vo.ParticipantId;
import ch.obya.pta.booking.domain.vo.SessionId;

@Provider
public class SessionAttributeConverterProvider implements ParamConverterProvider {

    private Map<Class, ParamConverter> converters = Map.of(
            SessionId.class, new SessionIdConverter(),
            ParticipantId.class, new ParticipantIdConverter()
    );

    @SuppressWarnings("unchecked")
    @Override
    public <T> ParamConverter<T> getConverter(Class<T> aClass, Type type, Annotation[] annotations) {
        return converters.getOrDefault(aClass, null);
    }

    static class SessionIdConverter implements ParamConverter<SessionId> {

        @Override
        public SessionId fromString(String value) {
            return new SessionId(UUID.fromString(value));
        }

        @Override
        public String toString(SessionId value) {
            return value.id().toString();
        }
    }

    static class ParticipantIdConverter implements ParamConverter<ParticipantId> {

        @Override
        public ParticipantId fromString(String value) {
            return new ParticipantId(UUID.fromString(value));
        }

        @Override
        public String toString(ParticipantId value) {
            return value.id().toString();
        }
    }
}
