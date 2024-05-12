package ch.obya.pta.customer.infrastructure.web;

import ch.obya.pta.customer.domain.aggregate.Customer;
import ch.obya.pta.customer.domain.vo.*;
import lombok.Builder;

@Builder(builderClassName = "Builder", toBuilder = true)
public record CustomerDto(
    CustomerId id,
    String salutation,
    Person.Name firstName,
    Person.Name lastName,
    Person.BirthDate birthDate,
    Person.Gender gender,
    PhysicalAddress deliveryAddress,
    PhysicalAddress billingAddress,
    EmailAddress emailAddress,
    PhoneNumber phoneNumber,
    String notes) {

    public static CustomerDto from(Customer customer) {
        var state = customer.state();
        return new CustomerDto(
                customer.id(),
                state.person().salutation(),
                state.person().firstName(),
                state.person().lastName(),
                state.person().birthDate(),
                state.person().gender(),
                state.deliveryAddress(),
                state.billingAddress(),
                state.emailAddress(),
                state.phoneNumber(),
                state.notes()
        );
    }
}
