package ch.obya.pta.customer.infrastructure.data;

import ch.obya.pta.common.domain.vo.Name;
import ch.obya.pta.common.util.search.AttributeFilter;
import ch.obya.pta.customer.domain.aggregate.Customer;
import ch.obya.pta.customer.domain.vo.*;
import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.quarkus.hibernate.reactive.panache.PanacheQuery;
import io.quarkus.panache.common.Parameters;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;

import java.time.LocalDate;
import java.util.ArrayList;
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
    @Enumerated(EnumType.STRING)
    Person.Gender gender;
    String streetNo;
    String zip;
    String city;
    String region;
    String country;
    String billingStreetNo;
    String billingZip;
    String billingCity;
    String billingRegion;
    String billingCountry;
    String email;
    String phone;
    String notes;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    List<PanacheSubscriptionEntity> subscriptions = new ArrayList<>();

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    // a Set: Hibernate cannot fetch two eager lists ("bags") of one entity at once
    java.util.Set<PanacheSubscriptionRequestEntity> requests = new java.util.HashSet<>();

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
        var customer = new Customer(
            new CustomerId(logicalId),
            new Person(salutation,
                    new Name(firstName),
                    new Name(lastName),
                    new Person.Birth(birth),
                    gender),
            new PhysicalAddress(streetNo, zip, city, region, country),
            billingZip == null ? null : new PhysicalAddress(billingStreetNo, billingZip, billingCity, billingRegion, billingCountry),
            email == null ? null : new EmailAddress(email),
            phone == null ? null : new PhoneNumber(phone),
            notes
        );
        subscriptions.forEach(s -> customer.add(s.toDomain()));
        requests.stream()
                .sorted(java.util.Comparator.comparing((PanacheSubscriptionRequestEntity r) -> r.requestedAt))
                .forEach(r -> customer.restore(r.toDomain()));
        // Rehydration is not a creation: drop the CustomerCreated event raised by the constructor.
        customer.domainEvents();
        return customer;
    }

    /**
     * Copies the customer's state and subscriptions. Subscription rows are updated in place: replacing them all
     * would insert the new rows before deleting the old ones (Hibernate's flush order) and violate their unique id.
     */
    PanacheCustomerEntity set(Customer customer) {
        set(customer.id(), customer.state());
        var incoming = customer.subscriptions().stream()
                .collect(java.util.stream.Collectors.toMap(s -> s.id().id(), s -> s));
        subscriptions.removeIf(row -> !incoming.containsKey(row.logicalId));
        subscriptions.forEach(row -> row.update(incoming.remove(row.logicalId)));
        incoming.values().forEach(s -> subscriptions.add(PanacheSubscriptionEntity.of(this, s)));
        // requests too: updated in place, added when new (they are never removed by the domain)
        var incomingRequests = customer.requests().stream()
                .collect(java.util.stream.Collectors.toMap(r -> r.id().id(), r -> r));
        requests.removeIf(row -> !incomingRequests.containsKey(row.logicalId));
        requests.forEach(row -> row.update(incomingRequests.remove(row.logicalId)));
        incomingRequests.values().forEach(r -> requests.add(PanacheSubscriptionRequestEntity.of(this, r)));
        return this;
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
        var billing = state.billingAddress();
        billingStreetNo = billing == null ? null : billing.streetNo();
        billingZip = billing == null ? null : billing.zip();
        billingCity = billing == null ? null : billing.city();
        billingRegion = billing == null ? null : billing.region();
        billingCountry = billing == null ? null : billing.country();
        email = state.emailAddress() == null ? null : state.emailAddress().address();
        phone = state.phoneNumber() == null ? null : state.phoneNumber().number();
        notes = state.notes();
        return this;
    }
}
