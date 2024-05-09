package ch.obya.pta.customer.domain.vo;

import ch.obya.pta.common.util.exception.CommonProblem;

import static ch.obya.pta.common.util.exception.CommonProblem.ifEmptyThrow;
import static ch.obya.pta.common.util.exception.CommonProblem.ifNullThrow;

public record PhysicalAddress(
        String streetNo,
        String zip,
        String city,
        String region,
        String country
) {

    public PhysicalAddress {
        ifNullThrow(zip, CommonProblem.AttributeNotNull.toException("PhysicalAddress.zip"));
        ifNullThrow(city, CommonProblem.AttributeNotNull.toException("PhysicalAddress.city"));
        ifNullThrow(country, CommonProblem.AttributeNotNull.toException("PhysicalAddress.country"));
        ifEmptyThrow(zip, CommonProblem.AttributeNotEmpty.toException("PhysicalAddress.zip"));
        ifNullThrow(city, CommonProblem.AttributeNotEmpty.toException("PhysicalAddress.city"));
        ifNullThrow(country, CommonProblem.AttributeNotEmpty.toException("PhysicalAddress.country"));
    }
}
