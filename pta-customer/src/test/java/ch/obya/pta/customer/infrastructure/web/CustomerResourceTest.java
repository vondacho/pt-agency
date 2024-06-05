package ch.obya.pta.customer.infrastructure.web;


import ch.obya.pta.common.domain.util.CommonProblem;
import ch.obya.pta.common.domain.vo.Name;
import ch.obya.pta.customer.application.CustomerService;
import ch.obya.pta.customer.domain.aggregate.Customer;
import ch.obya.pta.customer.domain.vo.*;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.Map;
import java.util.function.Consumer;

import static ch.obya.pta.customer.domain.util.Samples.oneCustomer;
import static ch.obya.pta.customer.domain.util.Samples.oneCustomerWithOneYearSubscription;
import static io.restassured.RestAssured.given;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@TestHTTPEndpoint(CustomerResource.class)
@QuarkusTest
class CustomerResourceTest {

    @InjectMock
    CustomerService customerService;

    @Test
    void look_up_non_existing_customer_resource_should_fail_with_404() {
        var customer = CustomerId.create();
        when(customerService.findOne(customer)).thenReturn(Uni.createFrom()
                .failure(CommonProblem.EntityNotFound.toException("Customer", customer.id()).get()));
        given()
                .when()
                .get("/{id}", Map.of("id", customer.id()))
                .then()
                .log().all()
                .statusCode(404)
                .body("detail", equalTo("Customer %s does not exist.".formatted(customer.id())));
    }

    @Test
    void look_up_existing_customer_resource_should_succeed_with_200() {
        var customer = oneCustomer.get();
        var state = customer.state();
        when(customerService.findOne(customer.id())).thenReturn(Uni.createFrom().item(customer));
        given()
                .when()
                .get("/{id}", Map.of("id", customer.id().id()))
                .then()
                .log().all()
                .statusCode(200)
                .body(
                        "id.id", equalTo(customer.id().toString()),
                        "firstName.content", equalTo(state.person().firstName().content()),
                        "lastName.content", equalTo(state.person().lastName().content())
                );
    }

    @Test
    void look_up_subscriptions_of_existing_customer_resource_should_succeed_with_200() {
        var customer = oneCustomerWithOneYearSubscription.get();
        var subscription = SubscriptionDto.from(customer.subscriptions().iterator().next());
        when(customerService.findOne(customer.id())).thenReturn(Uni.createFrom().item(customer));
        var result = given()
                .when()
                .queryParams("at", ISO_LOCAL_DATE.format(LocalDate.now()))
                .get("/{id}/subscriptions", Map.of("id", customer.id().id()))
                .then()
                .log().all()
                .statusCode(200)
                .body("size()", is(1))
                .extract()
                .as(SubscriptionDto[].class);
        assertThat(result[0]).isEqualTo(subscription);
    }

    @Test
    void create_customer_resource_should_succeed_with_201() {
        var dto = CustomerDto.from(oneCustomer.get()).toBuilder().id(null).build();
        var id = CustomerId.create();

        when(customerService.create(any(), any(), any(), any(), any(), any())).thenReturn(Uni.createFrom().item(id));

        given()
                .contentType(ContentType.JSON)
                .body(dto)
                .when()
                .post()
                .then()
                .log().all()
                .statusCode(201)
                .body("id", equalTo(id.id().toString()));

        var personCaptor = ArgumentCaptor.forClass(Person.class);
        verify(customerService).create(personCaptor.capture(), any(), any(), any(), any(), any());
        assertThat(personCaptor.getValue().firstName()).isEqualTo(dto.firstName());
        assertThat(personCaptor.getValue().lastName()).isEqualTo(dto.lastName());
        assertThat(personCaptor.getValue().birthDate()).isEqualTo(dto.birthDate());
        assertThat(personCaptor.getValue().gender()).isEqualTo(dto.gender());
    }

    @Test
    void modify_existing_customer_resource_should_succeed_with_204() {
        var customer = oneCustomer.get();
        when(customerService.modify(any(), any())).thenReturn(Uni.createFrom().voidItem());

        var dto = new CustomerDto.Builder()
                .firstName(new Name("albert"))
                .lastName(new Name("einstein"))
                .birthDate(new Person.BirthDate(LocalDate.of(1958, 12, 12)))
                .gender(Person.Gender.MALE)
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(dto)
                .when()
                .put("/{id}", Map.of("id", customer.id().id()))
                .then()
                .log().all()
                .statusCode(204);

        verifyModifier(customer, m -> {
            verify(m).rename(new Name("albert"), new Name("einstein"));
            verify(m).redefine(new Person.BirthDate(LocalDate.of(1958, 12, 12)), Person.Gender.MALE);
        });
    }

    @Test
    void partly_modify_existing_customer_resource_should_succeed_with_204() {
        var customer = oneCustomer.get();
        when(customerService.modify(any(), any())).thenReturn(Uni.createFrom().voidItem());

        given()
                .when()
                .queryParams("firstName", "albert", "lastName", "einstein")
                .queryParams("birthDate", "1958-12-12", "gender", "MALE")
                .patch("/{id}", Map.of("id", customer.id().id()))
                .then()
                .log().all()
                .statusCode(204);

        verifyModifier(customer, m -> {
            verify(m).rename(new Name("albert"), new Name("einstein"));
            verify(m).redefine(new Person.BirthDate(LocalDate.of(1958, 12, 12)), Person.Gender.MALE);
        });
    }

    @Test
    void rename_existing_customer_resource_should_succeed_with_204() {
        var customer = oneCustomer.get();
        when(customerService.modify(any(), any())).thenReturn(Uni.createFrom().voidItem());

        given()
                .when()
                .queryParams("firstName", "albert", "lastName", "einstein")
                .put("/{id}/naming", Map.of("id", customer.id().id()))
                .then()
                .log().all()
                .statusCode(204);

        verifyModifier(customer, m -> verify(m)
                .rename(new Name("albert"), new Name("einstein")));
    }

    @Test
    void redefine_existing_customer_resource_should_succeed_with_204() {
        var customer = oneCustomer.get();
        when(customerService.modify(any(), any())).thenReturn(Uni.createFrom().voidItem());

        given()
                .when()
                .queryParams("birthDate", "1958-12-12", "gender", "MALE")
                .put("/{id}/definition", Map.of("id", customer.id().id()))
                .then()
                .log().all()
                .statusCode(204);

        verifyModifier(customer, m -> verify(m)
                .redefine(new Person.BirthDate(LocalDate.of(1958, 12,12)), Person.Gender.MALE));
    }

    @Test
    void reconnect_existing_customer_resource_should_succeed_with_204() {
        var customer = oneCustomer.get();
        when(customerService.modify(any(), any())).thenReturn(Uni.createFrom().voidItem());

        given()
                .when()
                .queryParams("email", "albert@einstein.com", "phone", "+33123456789")
                .put("/{id}/connection", Map.of("id", customer.id().id()))
                .then()
                .log().all()
                .statusCode(204);

        verifyModifier(customer, m -> verify(m)
                .reconnect(new EmailAddress("albert@einstein.com"), new PhoneNumber("+33123456789")));
    }

    @Test
    void relocate_existing_customer_resource_should_succeed_with_204() {
        var customer = oneCustomer.get();
        when(customerService.modify(any(), any())).thenReturn(Uni.createFrom().voidItem());

        given()
                .when()
                .queryParams("delivery", "Bakerstreet 22,89000,London,London,UK")
                .put("/{id}/location", Map.of("id", customer.id().id()))
                .then()
                .log().all()
                .statusCode(204);

        verifyModifier(customer, m -> verify(m)
                .relocate(new PhysicalAddress("Bakerstreet 22", "89000", "London", "London", "UK"), null));
    }

    private void verifyModifier(Customer customer, Consumer<Customer.Modifier> verifier) {
        var modifierCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(customerService).modify(eq(customer.id()), modifierCaptor.capture());

        var modifierConsumer = modifierCaptor.getValue();
        var modifier = spy(customer.modify());
        modifierConsumer.accept(modifier);
        modifier.done();

        verifier.accept(modifier);
    }

    @Test
    void remove_existing_customer_resource_should_succeed_with_204() {
        var customer = oneCustomer.get();
        when(customerService.remove(customer.id(), true)).thenReturn(Uni.createFrom().voidItem());

        given()
                .when()
                .queryParams("force", "true")
                .delete("/{id}", Map.of("id", customer.id().id()))
                .then()
                .log().all()
                .statusCode(204);
    }
}