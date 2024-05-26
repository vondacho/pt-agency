package ch.obya.pta.customer.infrastructure.web;

import ch.obya.pta.common.domain.vo.Name;
import ch.obya.pta.common.util.search.FindCriteria;
import ch.obya.pta.customer.application.CustomerService;
import ch.obya.pta.customer.domain.repository.CustomerRepository;
import ch.obya.pta.customer.domain.vo.*;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import org.jboss.resteasy.reactive.ResponseStatus;
import org.jboss.resteasy.reactive.RestQuery;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;

import java.time.LocalDate;

@Produces("application/problem+json")
@Path("/customers")
public class CustomerResource {

    @Inject
    CustomerService service;
    @Inject
    CustomerRepository repository;

    @ResponseStatus(201)
    @POST
    public Uni<CustomerId> create(CustomerDto input) {
        return service.create(
                new Person(
                    input.salutation(),
                    input.firstName(),
                    input.lastName(),
                    input.birthDate(),
                    input.gender()
                ),
                input.deliveryAddress(),
                input.billingAddress(),
                input.emailAddress(),
                input.phoneNumber(),
                input.notes());
    }

    @ResponseStatus(204)
    @PUT
    @Path("/{id}")
    public Uni<Void> modify(CustomerId id, CustomerDto input) {
        return service.modify(id, customer -> customer
            .rename(input.firstName(), input.lastName())
            .redefine(input.birthDate(), input.gender())
            .reconnect(input.emailAddress(), input.phoneNumber())
            .relocate(input.deliveryAddress(), input.billingAddress())
            .annotate(input.notes()));
    }

    @ResponseStatus(204)
    @PATCH
    @Path("/{id}")
    public Uni<Void> modify(CustomerId id,
                            @RestQuery Name firstname, @RestQuery Name lastname,
                            @RestQuery Person.BirthDate birthdate, @RestQuery Person.Gender gender,
                            @RestQuery EmailAddress email, @RestQuery PhoneNumber phone,
                            @RestQuery PhysicalAddress delivery, @RestQuery PhysicalAddress billing,
                            @RestQuery String notes) {

        return service.modify(id, customer -> {
            if (firstname != null || lastname != null) customer.rename(firstname, lastname);
            if (birthdate != null || gender != null) customer.redefine(birthdate, gender);
            if (email != null || phone != null) customer.reconnect(email, phone);
            if (delivery != null || billing != null) customer.relocate(delivery, billing);
            if (notes != null) customer.annotate(notes);
        });
    }

    @ResponseStatus(204)
    @PUT
    @Path("/{id}/naming")
    public Uni<Void> rename(CustomerId id, @RestQuery Name firstname, @RestQuery Name lastname) {
        return service.modify(id, customer -> customer.rename(firstname, lastname));
    }

    @ResponseStatus(204)
    @PUT
    @Path("/{id}/definition")
    public Uni<Void> redefine(CustomerId id, @RestQuery Person.BirthDate birthdate, @RestQuery Person.Gender gender) {
        return service.modify(id, customer -> customer.redefine(birthdate, gender));
    }

    @ResponseStatus(204)
    @PUT
    @Path("/{id}/connection")
    public Uni<Void> reconnect(CustomerId id, @RestQuery EmailAddress email, @RestQuery PhoneNumber phone) {
        return service.modify(id, customer -> customer.reconnect(email, phone));
    }

    @ResponseStatus(204)
    @PUT
    @Path("/{id}/location")
    public Uni<Void> relocate(CustomerId id, @RestQuery PhysicalAddress delivery, @RestQuery PhysicalAddress billing) {
        return service.modify(id, customer -> customer.relocate(delivery, billing));
    }

    @GET
    @Path("/{id}")
    public Uni<CustomerDto> findOne(CustomerId id) {
        return service.findOne(id)
                .onFailure().transform(f -> Problem.valueOf(Status.NOT_FOUND, f.getMessage()))
                .map(CustomerDto::from);
    }

    @GET
    public Multi<CustomerDto> findByCriteria(@RestQuery String filter) {
        return repository.findByCriteria(filter.isEmpty() ? FindCriteria.empty() : FindCriteria.from(filter))
                .map(CustomerDto::from);
    }

    @GET
    @Path("/{id}/subscriptions")
    public Multi<SubscriptionDto> validSubscriptionsOf(CustomerId id, @RestQuery LocalDate at) {
        return service.findOne(id)
                .onFailure().transform(f -> Problem.valueOf(Status.NOT_FOUND, f.getMessage()))
                .onItem().transformToMulti(s -> Multi.createFrom().iterable(s.subscriptionsAt(at)))
                .map(SubscriptionDto::from);
    }

    @ResponseStatus(204)
    @DELETE
    @Path("/{id}")
    public Uni<Void> remove(CustomerId id, @RestQuery Boolean force) {
        return service.remove(id, force != null && force);
    }
}
