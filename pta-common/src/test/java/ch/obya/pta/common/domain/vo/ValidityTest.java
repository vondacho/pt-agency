package ch.obya.pta.common.domain.vo;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ValidityTest {

    static final LocalDate FROM = LocalDate.of(2026, 11, 1);
    static final LocalDate TO = LocalDate.of(2026, 11, 30);

    @Test
    void bounded_validity_includes_its_bounds_and_the_days_between() {
        var validity = new Validity(FROM, TO);
        assertThat(validity.includes(FROM)).isTrue();
        assertThat(validity.includes(LocalDate.of(2026, 11, 15))).isTrue();
        assertThat(validity.includes(TO)).isTrue();
        assertThat(validity.includes(FROM.minusDays(1))).isFalse();
        assertThat(validity.includes(TO.plusDays(1))).isFalse();
    }

    @Test
    void open_ended_validity_includes_every_day_from_its_start() {
        // regression: an open-ended validity used to include no day at all
        var validity = new Validity(FROM, null);
        assertThat(validity.includes(FROM)).isTrue();
        assertThat(validity.includes(LocalDate.of(2099, 1, 1))).isTrue();
        assertThat(validity.includes(FROM.minusDays(1))).isFalse();
    }

    @Test
    void validities_overlap_when_they_share_a_day() {
        var november = new Validity(FROM, TO);
        assertThat(november.overlappedBy(new Validity(TO, TO.plusDays(10)))).isTrue();
        assertThat(november.overlappedBy(new Validity(FROM.minusDays(10), FROM))).isTrue();
        assertThat(november.overlappedBy(new Validity(FROM.minusDays(10), TO.plusDays(10)))).isTrue();
        assertThat(november.overlappedBy(new Validity(TO.plusDays(1), null))).isFalse();
        assertThat(november.overlappedBy(new Validity(FROM.minusDays(10), FROM.minusDays(1)))).isFalse();
    }

    @Test
    void open_ended_validities_overlap_everything_after_their_start() {
        // regression: comparing with an open-ended validity used to throw
        var fromNovember = new Validity(FROM, null);
        assertThat(fromNovember.overlappedBy(new Validity(FROM.minusDays(10), FROM.minusDays(1)))).isFalse();
        assertThat(fromNovember.overlappedBy(new Validity(FROM.minusDays(10), FROM))).isTrue();
        assertThat(fromNovember.overlappedBy(new Validity(LocalDate.of(2030, 1, 1), null))).isTrue();
        assertThat(new Validity(FROM.minusDays(10), FROM.minusDays(1)).overlappedBy(fromNovember)).isFalse();
    }
}
