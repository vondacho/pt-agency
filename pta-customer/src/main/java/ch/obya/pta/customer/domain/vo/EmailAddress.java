package ch.obya.pta.customer.domain.vo;

import ch.obya.pta.common.domain.util.CommonProblem;
import ch.obya.pta.common.util.validation.Checker;
import ch.obya.pta.customer.domain.util.CustomerProblem;

import java.util.regex.Pattern;

import static ch.obya.pta.common.domain.util.CommonProblem.ifEmptyThrow;
import static ch.obya.pta.common.domain.util.CommonProblem.ifNullThrow;

public record EmailAddress(String address) {

    public static Pattern EMAIL = Pattern.compile(
            "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$");

    public EmailAddress {
        ifNullThrow(address, CommonProblem.AttributeNotNull.toException("EmailAddress.address"));
        ifEmptyThrow(address, CommonProblem.AttributeNotEmpty.toException("EmailAddress.address"));
    }

    public static class EmailAddressChecker implements Checker<EmailAddress> {
        public EmailAddress check(EmailAddress emailAddress) {
            if (!EMAIL.matcher(emailAddress.address).matches())
                throw CustomerProblem.EmailAddressInvalid.toException(emailAddress.address, EMAIL.pattern()).get();
            return emailAddress;
        }
    }
}
