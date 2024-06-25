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
        Person.Birth birth,
        Person.Gender gender,
        PhysicalAddress delivery,
        PhysicalAddress billing,
        EmailAddress email,
        PhoneNumber phone,
        String notes) {

    public static CustomerDto from(Customer customer) {
        var state = customer.state();
        return CustomerDto.builder()
                .id(customer.id())
                .salutation(state.person().salutation())
                .firstName(state.person().firstName())
                .lastName(state.person().lastName())
                .birth(state.person().birth())
                .gender(state.person().gender())
                .delivery(state.deliveryAddress())
                .billing(state.billingAddress())
                .email(state.emailAddress())
                .phone(state.phoneNumber())
                .notes(state.notes()).build();
    }
}
