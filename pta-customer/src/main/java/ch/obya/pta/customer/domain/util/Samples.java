package ch.obya.pta.customer.domain.util;

import ch.obya.pta.common.domain.vo.Name;
import ch.obya.pta.common.domain.vo.Quota;
import ch.obya.pta.common.domain.vo.Validity;
import ch.obya.pta.customer.domain.aggregate.Customer;
import ch.obya.pta.customer.domain.entity.Subscription;
import ch.obya.pta.customer.domain.vo.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.function.Supplier;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Samples {

    public static Customer johnDoe() {
        return new Customer(
                new Person("Mr",
                        new Name("john"),
                        new Name("doe"),
                        new Person.Birth(LocalDate.of(1966, 4, 5)),
                        Person.Gender.MALE),
                new PhysicalAddress("test-delivery", "zip", "city", "region", "ch"),
                new PhysicalAddress("test-billing", "zip", "city", "region", "ch"),
                new EmailAddress("john@doe.ch"),
                new PhoneNumber("+41781234567"),
                "no notes");
    }

    public static Customer sherlockHolmes() {
        return new Customer(
                new Person("Mr",
                        new Name("sherlock"),
                        new Name("holmes"),
                        new Person.Birth(LocalDate.of(1966, 4, 5)),
                        Person.Gender.MALE),
                new PhysicalAddress("221B Baker St", "NW1 6XE", "london", "Marylebone", "uk"),
                new PhysicalAddress("221B Baker St", "NW1 6XE", "london", "Marylebone", "uk"),
                new EmailAddress("sherlock@holmes.uk"),
                new PhoneNumber("+4402072243688"),
                "no comment");
    }

    private static Subscription subscription() {
        var today = LocalDate.now();
        return new Subscription(
                SubscriptionId.create(),
                new Validity(today, today.plusMonths(12)),
                new Quota(null, 10),
                9,
                ArticleId.create());
    }

    public static final Supplier<Customer> oneCustomer = Samples::johnDoe;

    public static final Supplier<Customer> oneCustomerWithOneYearSubscription = () -> {
        var c = oneCustomer.get();
        c.add(subscription());
        return c;
    };
}
