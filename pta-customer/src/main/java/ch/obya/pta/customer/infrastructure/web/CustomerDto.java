package ch.obya.pta.customer.infrastructure.web;

import ch.obya.pta.common.domain.vo.Name;
import ch.obya.pta.customer.domain.aggregate.Customer;
import ch.obya.pta.customer.domain.vo.*;
import lombok.Builder;

@Builder(builderClassName = "Builder", toBuilder = true)
public record CustomerDto(
    CustomerId id,
    String salutation,
    Name firstName,
    Name lastName,
    Person.BirthDate birthDate,
    Person.Gender gender,
    PhysicalAddress deliveryAddress,
    PhysicalAddress billingAddress,
    EmailAddress emailAddress,
    PhoneNumber phoneNumber,
    String notes) {

    public static CustomerDto from(Customer customer) {
        var state = customer.state();
        return CustomerDto.builder()
                .id(customer.id())
                .salutation(state.person().salutation())
                .firstName(                state.person().firstName())
                .lastName(                state.person().lastName())
                .birthDate(                state.person().birthDate())
                .gender(                state.person().gender())
                .deliveryAddress(                state.deliveryAddress())
                .billingAddress(                state.billingAddress())
                .emailAddress(                state.emailAddress())
                .phoneNumber(                state.phoneNumber())
                .notes(                state.notes()).build();
    }
}
