package ch.obya.pta.customer.infrastructure.web;

import ch.obya.pta.customer.domain.vo.*;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.UUID;

@Provider
public class CustomerAttributeConverterProvider implements ParamConverterProvider {

    private final Map<Class, ParamConverter> converters = Map.of(
            CustomerId.class, new CustomerIdConverter(),
            Person.Birth.class, new BirthDateConverter(),
            EmailAddress.class, new EmailAddressConverter(),
            PhoneNumber.class, new PhoneNumberConverter(),
            PhysicalAddress.class, new PhysicalAddressConverter()
    );

    @SuppressWarnings("unchecked")
    @Override
    public <T> ParamConverter<T> getConverter(Class<T> aClass, Type type, Annotation[] annotations) {
        return converters.getOrDefault(aClass, null);
    }

    private static class BirthDateConverter implements ParamConverter<Person.Birth> {
        @Override
        public Person.Birth fromString(String value) {
            return Person.Birth.fromISO(value);
        }

        @Override
        public String toString(Person.Birth value) {
            return value.toISO();
        }
    }

    private static class CustomerIdConverter implements ParamConverter<CustomerId> {
        @Override
        public CustomerId fromString(String value) {
            return new CustomerId(UUID.fromString(value));
        }

        @Override
        public String toString(CustomerId value) {
            return value.id().toString();
        }
    }

    private static class EmailAddressConverter implements ParamConverter<EmailAddress> {
        @Override
        public EmailAddress fromString(String value) {
            return new EmailAddress(value);
        }

        @Override
        public String toString(EmailAddress value) {
            return value.address();
        }
    }

    private static class PhoneNumberConverter implements ParamConverter<PhoneNumber> {
        @Override
        public PhoneNumber fromString(String value) {
            return new PhoneNumber(value);
        }

        @Override
        public String toString(PhoneNumber value) {
            return value.toString();
        }
    }

    private static class PhysicalAddressConverter implements ParamConverter<PhysicalAddress> {
        @Override
        public PhysicalAddress fromString(String value) {
            var fields = value.split(",");
            return new PhysicalAddress(fields[0], fields[1], fields[2], fields[3], fields[4]);
        }

        @Override
        public String toString(PhysicalAddress value) {
            return "%s,%s,%s,%s,%s".formatted(value.streetNo(), value.zip(), value.city(), value.region(), value.country());
        }
    }
}
