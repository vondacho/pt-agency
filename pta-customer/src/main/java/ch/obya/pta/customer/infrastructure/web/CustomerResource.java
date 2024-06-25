package ch.obya.pta.customer.infrastructure.web;

import ch.obya.pta.common.domain.vo.Name;
import ch.obya.pta.common.util.search.AttributeFilter;
import ch.obya.pta.customer.application.CustomerService;
import ch.obya.pta.customer.domain.repository.CustomerRepository;
import ch.obya.pta.customer.domain.vo.*;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import org.jboss.resteasy.reactive.ResponseStatus;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                    input.birth(),
                    input.gender()
                ),
                input.delivery(),
                input.billing(),
                input.email(),
                input.phone(),
                input.notes())
                .onFailure().transform(t -> Problem.valueOf(Status.BAD_REQUEST, t.getMessage()));
    }

    @ResponseStatus(204)
    @PUT
    @Path("/{id}")
    public Uni<Void> replace(@RestPath CustomerId id, CustomerDto input) {
        return service.modify(id, customer -> customer
            .rename(input.firstName(), input.lastName())
            .redefine(input.birth(), input.gender())
            .reconnect(input.email(), input.phone())
            .relocate(input.delivery(), input.billing())
            .annotate(input.notes()))
                .onFailure().transform(t -> Problem.valueOf(Status.BAD_REQUEST, t.getMessage()));
    }

    @ResponseStatus(204)
    @PATCH
    @Path("/{id}")
    public Uni<Void> update(@RestPath CustomerId id,
                            @RestQuery Name firstname, @RestQuery Name lastname,
                            @RestQuery Person.Birth birth, @RestQuery Person.Gender gender,
                            @RestQuery EmailAddress email, @RestQuery PhoneNumber phone,
                            @RestQuery PhysicalAddress delivery, @RestQuery PhysicalAddress billing,
                            @RestQuery String notes) {

        return service.modify(id, customer -> {
            if (firstname != null || lastname != null) customer.rename(firstname, lastname);
            if (birth != null || gender != null) customer.redefine(birth, gender);
            if (email != null || phone != null) customer.reconnect(email, phone);
            if (delivery != null || billing != null) customer.relocate(delivery, billing);
            if (notes != null) customer.annotate(notes);
        }).onFailure().transform(t -> Problem.valueOf(Status.BAD_REQUEST, t.getMessage()));
    }

    @ResponseStatus(204)
    @PATCH
    @Path("/{id}/naming")
    public Uni<Void> rename(@RestPath CustomerId id, @RestQuery Name firstname, @RestQuery Name lastname) {
        return service.modify(id, customer -> customer.rename(firstname, lastname))
                .onFailure().transform(t -> Problem.valueOf(Status.BAD_REQUEST, t.getMessage()));
    }

    @ResponseStatus(204)
    @PATCH
    @Path("/{id}/definition")
    public Uni<Void> redefine(@RestPath CustomerId id, @RestQuery Person.Birth birth, @RestQuery Person.Gender gender) {
        return service.modify(id, customer -> customer.redefine(birth, gender))
                .onFailure().transform(t -> Problem.valueOf(Status.BAD_REQUEST, t.getMessage()));
    }

    @ResponseStatus(204)
    @PATCH
    @Path("/{id}/connection")
    public Uni<Void> reconnect(@RestPath CustomerId id, @RestQuery EmailAddress email, @RestQuery PhoneNumber phone) {
        return service.modify(id, customer -> customer.reconnect(email, phone))
                .onFailure().transform(t -> Problem.valueOf(Status.BAD_REQUEST, t.getMessage()));
    }

    @ResponseStatus(204)
    @PATCH
    @Path("/{id}/location")
    public Uni<Void> relocate(@RestPath CustomerId id, @RestQuery PhysicalAddress delivery, @RestQuery PhysicalAddress billing) {
        return service.modify(id, customer -> customer.relocate(delivery, billing))
                .onFailure().transform(t -> Problem.valueOf(Status.BAD_REQUEST, t.getMessage()));
    }

    @GET
    @Path("/{id}")
    public Uni<CustomerDto> get(@RestPath CustomerId id) {
        return service.findOne(id)
                .onFailure().transform(f -> Problem.valueOf(Status.NOT_FOUND, f.getMessage()))
                .map(CustomerDto::from);
    }

    @GET
    public Uni<List<CustomerDto>> search(@RestQuery List<String> firstname,
                                         @RestQuery List<String> lastname,
                                         @RestQuery List<String> birth,
                                         @RestQuery List<String> gender,
                                         @RestQuery List<String> email,
                                         @RestQuery List<String> phone) {

        Map<String, List<AttributeFilter>> criteria = new HashMap<>();
        if (!firstname.isEmpty()) criteria.put("firstName", AttributeFilter.from(firstname, Name::new));
        if (!lastname.isEmpty()) criteria.put("lastName", AttributeFilter.from(lastname, Name::new));
        if (!birth.isEmpty()) criteria.put("birth", AttributeFilter.from(birth, Person.Birth::fromISO));
        if (!gender.isEmpty()) criteria.put("gender", AttributeFilter.from(gender, Person.Gender::valueOf));
        if (!email.isEmpty()) criteria.put("email", AttributeFilter.from(email, EmailAddress::new));
        if (!phone.isEmpty()) criteria.put("phone", AttributeFilter.from(phone, PhoneNumber::new));

        return repository.findByCriteria(criteria)
                .map(l -> l.stream().map(CustomerDto::from).toList())
                .onFailure().transform(t -> Problem.valueOf(Status.BAD_REQUEST, t.getMessage()));
    }

    @GET
    @Path("/{id}/subscriptions")
    public Multi<SubscriptionDto> validSubscriptionsOf(@RestPath CustomerId id, @RestQuery LocalDate at) {
        return service.findOne(id)
                .onFailure().transform(f -> Problem.valueOf(Status.NOT_FOUND, f.getMessage()))
                .onItem().transformToMulti(s -> Multi.createFrom().iterable(s.subscriptionsAt(at)))
                .map(SubscriptionDto::from)
                .onFailure().transform(t -> Problem.valueOf(Status.BAD_REQUEST, t.getMessage()));
    }

    @ResponseStatus(204)
    @DELETE
    @Path("/{id}")
    public Uni<Void> remove(@RestPath CustomerId id, @RestQuery Boolean force) {
        return service.remove(id, force != null && force)
                .onFailure().transform(t -> Problem.valueOf(Status.BAD_REQUEST, t.getMessage()));
    }
}
