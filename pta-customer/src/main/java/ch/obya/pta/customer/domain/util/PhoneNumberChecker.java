package ch.obya.pta.customer.domain.util;

import ch.obya.pta.common.util.validation.Checker;
import ch.obya.pta.customer.domain.vo.PhoneNumber;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberMatch;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.Optional;

import static ch.obya.pta.customer.domain.vo.PhoneNumber.PHONE_CH;
import static ch.obya.pta.customer.domain.vo.PhoneNumber.PHONE_INTERNATIONAL;
import static com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberType.UNKNOWN;

@Slf4j
public class PhoneNumberChecker implements Checker<PhoneNumber> {

    private static final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
    private static final String DEFAULT_REGION = "CH";

    @Override
    public PhoneNumber check(PhoneNumber input) {
        return sanitize(input).orElseThrow(
                CustomerProblem.PhoneNumberInvalid.toException(input.number(), PHONE_CH.pattern(), PHONE_INTERNATIONAL.pattern()));
    }

    public Optional<PhoneNumber> sanitize(PhoneNumber phoneNumber) {
        return Optional.ofNullable(phoneNumber)
                .map(PhoneNumber::number)
                .flatMap(this::extract)
                .flatMap(this::parse)
                .flatMap(this::canonize);
    }

    private Optional<String> extract(String phoneNumber) {
        Iterator<PhoneNumberMatch> it = phoneNumberUtil.findNumbers(
                phoneNumber, DEFAULT_REGION, PhoneNumberUtil.Leniency.POSSIBLE, Long.MAX_VALUE).iterator();
        return it.hasNext() ? Optional.of(it.next().rawString()) : Optional.empty();
    }

    private Optional<Phonenumber.PhoneNumber> parse(String phoneNumber) {
        try {
            return Optional.of(phoneNumberUtil.parse(phoneNumber, DEFAULT_REGION));
        } catch (NumberParseException e) {
            LOG.debug("{} does not contain a valid phone number that can be parsed", phoneNumber);
            return Optional.empty();
        }
    }

    private Optional<PhoneNumber> canonize(Phonenumber.PhoneNumber phoneNumber) {
        if (phoneNumberUtil.isValidNumber(phoneNumber)) {
            final PhoneNumberUtil.PhoneNumberType parsedNumberType = phoneNumberUtil.getNumberType(phoneNumber);
            checkType(phoneNumber, parsedNumberType);
            final String canonizedNumber = phoneNumberUtil.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL);
            LOG.debug("{} has been canonized as {} of type {}", phoneNumber, canonizedNumber, parsedNumberType);
            return Optional.of(new PhoneNumber(canonizedNumber));
        }
        else {
            LOG.debug("{} does not contain a valid phone number", phoneNumber);
            return Optional.empty();
        }
    }

    private void checkType(Phonenumber.PhoneNumber phoneNumber, PhoneNumberUtil.PhoneNumberType type) {
        if (UNKNOWN == type) {
            LOG.warn("{} seems to be an unknown phone number", phoneNumber);
        }
    }
}
