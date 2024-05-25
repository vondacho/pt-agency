package ch.obya.pta.customer.infrastructure.web;

import ch.obya.pta.common.domain.vo.Name;
import ch.obya.pta.common.domain.vo.Quota;
import ch.obya.pta.customer.domain.vo.*;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;

@Provider
public class CustomerAttributeConverterProvider implements ParamConverterProvider {

    private final Map<Class, ParamConverter> converters = Map.of(
            CustomerId.class, new CustomerIdConverter(),
            Name.class, new NameConverter(),
            Person.BirthDate.class, new BirthDateConverter(),
            EmailAddress.class, new EmailAddressConverter(),
            PhoneNumber.class, new PhoneNumberConverter(),
            PhysicalAddress.class, new PhysicalAddressConverter(),
            Quota.class, new QuotaConverter()
    );

    @SuppressWarnings("unchecked")
    @Override
    public <T> ParamConverter<T> getConverter(Class<T> aClass, Type type, Annotation[] annotations) {
        return converters.getOrDefault(aClass, null);
    }

    private static class BirthDateConverter implements ParamConverter<Person.BirthDate> {
        @Override
        public Person.BirthDate fromString(String value) {
            return new Person.BirthDate(LocalDate.parse(value, ISO_LOCAL_DATE));
        }

        @Override
        public String toString(Person.BirthDate value) {
            return ISO_LOCAL_DATE.format(value.date());
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
