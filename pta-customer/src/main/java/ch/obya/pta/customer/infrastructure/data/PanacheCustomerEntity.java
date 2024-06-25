package ch.obya.pta.customer.infrastructure.data;

import ch.obya.pta.common.domain.vo.Name;
import ch.obya.pta.common.util.search.AttributeFilter;
import ch.obya.pta.customer.domain.aggregate.Customer;
import ch.obya.pta.customer.domain.vo.*;
import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.quarkus.hibernate.reactive.panache.PanacheQuery;
import io.quarkus.panache.common.Parameters;
import jakarta.persistence.Entity;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity(name = "customer")
class PanacheCustomerEntity extends PanacheEntity {
    UUID logicalId;
    String salutation;
    String firstName;
    String lastName;
    LocalDate birth;
    Person.Gender gender;
    String streetNo;
    String zip;
    String city;
    String region;
    String country;
    String email;
    String phone;

    static PanacheQuery<PanacheCustomerEntity> findByLogicalId(CustomerId id) {
        return find("logicalId", id.id());
    }

    static PanacheQuery<PanacheCustomerEntity> findByCriteria(Map<String, List<AttributeFilter>> criteria) {
        return find(toQuery(criteria), toParameters(criteria));
    }

    private static String toQuery(Map<String, List<AttributeFilter>> criteria) {
        return criteria.entrySet().stream()
                .map(e -> toComparison(e.getKey(), e.getValue()))
                .reduce("%s and %s"::formatted)
                .orElse("");
    }

    private static String toComparison(String name, List<AttributeFilter> value) {
        return value.stream()
                .map(f -> "%s %s :%s".formatted(toDataField(name), toOperator(f.operator()), toDataField(name)))
                .reduce("%s and %s"::formatted)
                .orElse("");
    }

    private static String toOperator(AttributeFilter.Operator operator) {
        return switch (operator) {
            case EQUALS -> "=";
            case NOT_EQUALS -> "!=";
            case GREATER_THAN -> ">";
            case GREATER_THAN_EQUALS -> ">=";
            case LESS_THAN -> "<";
            case LESS_THAN_EQUALS -> "<=";
            case LIKE -> "like";
        };
    }

    private static Parameters toParameters(Map<String, List<AttributeFilter>> criteria) {
        var parameters = new Parameters();
        for(var filters: criteria.entrySet()) {
            for (var filter: filters.getValue()) {
                parameters = parameters.and(toDataField(filters.getKey()), toParameter(filter.value()));
            }
        }
        return parameters;
    }

    private static Object toParameter(Object value) {
        return switch (value) {
            case CustomerId id -> id.id();
            case Name name -> name.content();
            case PhoneNumber phoneNumber -> phoneNumber.number();
            case EmailAddress emailAddress -> emailAddress.address();
            case Person.Birth birth -> birth.date();
            default -> value;
        };
    }

    private static String toDataField(String name) {
        return switch (name) {
            case "id" -> "logicalId";
            case "lastName" -> "lastName";
            case "firstName" -> "firstName";
            case "birth" -> "birth";
            case "gender" -> "gender";
            case "streetNo" -> "streetNo";
            case "zip" -> "zip";
            case "city" -> "city";
            case "region" -> "region";
            case "country" -> "country";
            case "email" -> "email";
            case "phone" -> "phone";
            default -> throw new IllegalArgumentException("Unknown attribute: " + name);
        };
    }

    Customer toDomain() {
        return new Customer(
            new CustomerId(logicalId),
            new Person(salutation,
                    new Name(firstName),
                    new Name(lastName),
                    new Person.Birth(birth),
                    gender),
            new PhysicalAddress(streetNo, zip, city, region, country),
            null,
            new EmailAddress("john@doe.ch"),
            new PhoneNumber("0041781234567"),
            "notes"
        );
    }

    PanacheCustomerEntity set(CustomerId id, Customer.State state) {
        logicalId = id.id();
        salutation = state.person().salutation();
        firstName = state.person().firstName().content();
        lastName = state.person().lastName().content();
        birth = state.person().birth().date();
        gender = state.person().gender();
        streetNo = state.deliveryAddress().streetNo();
        zip = state.deliveryAddress().zip();
        city = state.deliveryAddress().city();
        region = state.deliveryAddress().region();
        country = state.deliveryAddress().country();
        email = state.emailAddress().address();
        phone = state.phoneNumber().number();
        return this;
    }
}
