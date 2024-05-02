package ch.obya.pta.booking.web;

import ch.obya.pta.booking.domain.ParticipantId;
import ch.obya.pta.booking.domain.SessionId;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.UUID;

public class ParticipantIdConverter implements ParamConverter<ParticipantId> {

    @Override
    public ParticipantId fromString(String value) {
        return new ParticipantId(UUID.fromString(value));
    }

    @Override
    public String toString(ParticipantId value) {
        return value.id().toString();
    }

    @Provider
    public static class ParticipantIdConverterProvider implements ParamConverterProvider {
        @SuppressWarnings("unchecked")
        @Override
        public <T> ParamConverter<T> getConverter(Class<T> aClass, Type type, Annotation[] annotations) {
            return type == ParticipantId.class ? (ParamConverter<T>)new ParticipantIdConverter() : null;
        }
    }
}
