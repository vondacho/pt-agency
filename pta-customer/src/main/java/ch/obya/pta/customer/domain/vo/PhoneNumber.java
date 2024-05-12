package ch.obya.pta.customer.domain.vo;

import ch.obya.pta.common.domain.util.CommonProblem;

import java.util.regex.Pattern;

import static ch.obya.pta.common.domain.util.CommonProblem.ifEmptyThrow;
import static ch.obya.pta.common.domain.util.CommonProblem.ifNullThrow;

public record PhoneNumber(String number) {

    public static Pattern PHONE_CH = Pattern.compile(
            "(?:(?:|0{1,2}|\\+{0,2})41(?:|\\(0\\))|0)(| )([1-9]\\d)(| )(\\d{3})(| )(\\d{2})(| )(\\d{2})");
    public static Pattern PHONE_INTERNATIONAL = Pattern.compile("\\+(?:[0-9] ?){6,14}[0-9]");

    public PhoneNumber {
        ifNullThrow(number, CommonProblem.AttributeNotNull.toException("PhoneNumber.number"));
        ifEmptyThrow(number, CommonProblem.AttributeNotEmpty.toException("PhoneNumber.number"));
    }
}