package ch.obya.pta.customer.domain.util;


import ch.obya.pta.common.domain.util.Problem;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum CustomerProblem implements Problem {

    NoCustomer("Customer %s does not exist."),
    BirthDateInvalid("Birth date %s is not valid and cannot be in the future."),
    EmailAddressInvalid("Email address %s is not valid and should match %s."),
    PhoneNumberInvalid("Phone number %s is not valid and should match %s or %s."),
    SubscriptionWithoutCredit("Subscription %s does not have credits."),
    SubscriptionOverlapsExisting("Subscription %s overlaps existing subscription %s."),
    AlreadyExisting("Customer (%s,%s,%s,%s) already exists.");

    private final String text;
}
