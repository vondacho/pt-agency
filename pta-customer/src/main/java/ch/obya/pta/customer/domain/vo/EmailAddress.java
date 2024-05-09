package ch.obya.pta.customer.domain.vo;

import ch.obya.pta.common.util.exception.CommonProblem;
import ch.obya.pta.common.util.validation.Checker;

import java.util.regex.Pattern;

import static ch.obya.pta.common.util.exception.CommonProblem.ifEmptyThrow;
import static ch.obya.pta.common.util.exception.CommonProblem.ifNullThrow;

public record EmailAddress(String address) {

    public static Pattern EMAIL = Pattern.compile("[a-zA-Z0-9]+(?:\\.[a-zA-Z0-9]+)*@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9]+)+");

    public EmailAddress {
        ifNullThrow(address, CommonProblem.AttributeNotNull.toException("EmailAddress.address"));
        ifEmptyThrow(address, CommonProblem.AttributeNotEmpty.toException("EmailAddress.address"));
    }

    public static class EmailAddressChecker implements Checker<EmailAddress> {
        public EmailAddress check(EmailAddress emailAddress) {
//            if (!EMAIL.matcher(emailAddress.address).hasMatch())
//                throw CustomerProblem.EmailAddressInvalid.toException(emailAddress.address, EMAIL.pattern()).get();
            return emailAddress;
        }
    }
}
