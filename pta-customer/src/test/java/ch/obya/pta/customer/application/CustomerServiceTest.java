package ch.obya.pta.customer.application;


import ch.obya.pta.common.application.EventPublisher;
import ch.obya.pta.common.domain.event.Event;
import ch.obya.pta.common.domain.util.CommonProblem;
import ch.obya.pta.customer.domain.aggregate.Customer;
import ch.obya.pta.customer.domain.event.CustomerCreated;
import ch.obya.pta.customer.domain.event.CustomerModified;
import ch.obya.pta.customer.domain.event.CustomerRemoved;
import ch.obya.pta.customer.domain.repository.CustomerRepository;
import ch.obya.pta.customer.domain.vo.CustomerId;
import ch.obya.pta.customer.domain.util.Samples;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
public class CustomerServiceTest {

    @Mock
    EventPublisher eventPublisher;
    @Mock
    CustomerRepository customerRepository;
    @InjectMocks
    CustomerService customerService;
    @Captor
    ArgumentCaptor<Collection<Event>> eventCaptor;
    @Captor
    ArgumentCaptor<Customer> persistedCustomerCaptor;

    @Test
    void look_up_non_existing_customer_should_fail_with_expected_exception() {
        var customer = CustomerId.create();

        when(customerRepository.findOne(customer)).thenReturn(Uni.createFrom().nothing());

        var result = customerService.findOne(customer);

        result.subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitFailure()
                .assertFailedWith(CommonProblem.Exception.class, "Customer %s does not exist.".formatted(customer));
    }

    @Test
    void look_up_existing_customer_should_return_expected_entity() {
        var customer = Samples.oneCustomer.get();

        when(customerRepository.findOne(customer.id())).thenReturn(Uni.createFrom().item(customer));

        var result = customerService.findOne(customer.id()).subscribe().withSubscriber(UniAssertSubscriber.create()).getItem();

        assertThat(result.id()).isEqualTo(customer.id());
    }

    @Test
    void create_customer_should_persist_and_publish_and_return_expected_id() {
        var template = Samples.oneCustomer.get().state();

        when(customerRepository.findByCriteria(anyMap())).thenReturn(Uni.createFrom().item(Collections.emptyList()));
        when(customerRepository.save(any()))
                .thenAnswer(invocation -> Uni.createFrom().item(invocation.getArgument(0, Customer.class)));
        when(eventPublisher.publish(anyCollection())).thenReturn(Uni.createFrom().voidItem());

        var result = customerService.create(
                template.person(),
                template.deliveryAddress(),
                template.billingAddress(),
                template.emailAddress(),
                template.phoneNumber(),
                template.notes()).subscribe().withSubscriber(UniAssertSubscriber.create()).getItem();

        verify(customerRepository, times(1)).save(persistedCustomerCaptor.capture());

        verify(eventPublisher, times(1)).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("timestamp")
                .containsExactly(new CustomerCreated(persistedCustomerCaptor.getValue().id()));

        assertThat(result).isEqualTo(persistedCustomerCaptor.getValue().id());
    }

    @Test
    void create_duplicate_customer_should_fail() {
        var template = Samples.oneCustomer.get().state();

        when(customerRepository.findByCriteria(anyMap())).thenReturn(Uni.createFrom().item(List.of(Samples.oneCustomer.get())));

        var result = customerService.create(
                template.person(),
                template.deliveryAddress(),
                template.billingAddress(),
                template.emailAddress(),
                template.phoneNumber(),
                template.notes());

        result.subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitFailure()
                .assertFailedWith(CommonProblem.Exception.class,
                        "Customer (%s,%s,%s,%s) already exists.".formatted(
                                template.person().firstName(),
                                template.person().lastName(),
                                template.person().birth(),
                                template.emailAddress()));
    }

    @Test
    void modify_existing_customer_should_apply_and_persist_and_publish_and_return_expected_id() {
        var customer = Samples.oneCustomer.get();

        when(customerRepository.findOne(customer.id())).thenReturn(Uni.createFrom().item(customer));
        when(customerRepository.findByCriteria(anyMap())).thenReturn(Uni.createFrom().item(Collections.emptyList()));
        when(customerRepository.save(any()))
                .thenAnswer(invocation -> Uni.createFrom().item(invocation.getArgument(0, Customer.class)));
        when(eventPublisher.publish(anyCollection())).thenReturn(Uni.createFrom().voidItem());

        customerService.modify(customer.id(), m -> m.annotate("modified").done())
                .subscribe().withSubscriber(UniAssertSubscriber.create()).getItem();

        verify(customerRepository, times(1)).findOne(customer.id());
        verify(customerRepository, times(1)).save(persistedCustomerCaptor.capture());
        assertThat(persistedCustomerCaptor.getValue().id()).isEqualTo(customer.id());
        assertThat(persistedCustomerCaptor.getValue().state().notes()).isEqualTo("modified");

        verify(eventPublisher, times(1)).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("timestamp")
                .contains(new CustomerModified(customer.id()));
    }

    @Test
    void modified_customer_conflicting_with_existing_one_should_fail() {
        var customer = Samples.oneCustomer.get();
        var state = customer.state();

        when(customerRepository.findOne(customer.id())).thenReturn(Uni.createFrom().item(customer));
        when(customerRepository.findByCriteria(anyMap())).thenReturn(Uni.createFrom().item(List.of(Samples.oneCustomer.get())));

        var result = customerService.modify(customer.id(), m -> m.annotate("modified").done());

        result.subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitFailure()
                .assertFailedWith(CommonProblem.Exception.class,
                        "Customer (%s,%s,%s,%s) already exists.".formatted(
                                state.person().firstName(),
                                state.person().lastName(),
                                state.person().birth(),
                                state.emailAddress()));
    }

    @Test
    void remove_existing_customer_should_delete_and_publish() {
        var customer = Samples.oneCustomer.get();

        when(customerRepository.findOne(customer.id())).thenReturn(Uni.createFrom().item(customer));
        when(customerRepository.remove(customer.id())).thenReturn(Uni.createFrom().voidItem());
        when(eventPublisher.publish(anyCollection())).thenReturn(Uni.createFrom().voidItem());

        customerService.remove(customer.id(), true).subscribe().withSubscriber(UniAssertSubscriber.create()).getItem();

        verify(customerRepository, times(1)).findOne(customer.id());
        verify(customerRepository, times(1)).remove(customer.id());

        verify(eventPublisher, times(1)).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("timestamp")
                .contains(new CustomerRemoved(customer.id()));
    }
}
