package ch.obya.pta.booking;

import io.cucumber.java.en.Then;

import ch.obya.pta.booking.domain.aggregate.Session;
import ch.obya.pta.booking.domain.entity.Booking;
import ch.obya.pta.booking.domain.vo.ParticipantId;

import static ch.obya.pta.booking.TestContext.at;
import static org.assertj.core.api.Assertions.assertThat;

public class AssertionSteps {

    @Then("{} has one booking for {} with status {}")
    public void ps_has_one_booking_for_p_with_status(String session, String participant, String status) {
        Session s = at(session);
        ParticipantId p = at(participant);
        Booking.Status bs = Booking.Status.valueOf(status);
        assertThat(s.bookings()).anyMatch(b -> b.id().participant().equals(p) && b.status() == bs);
    }

    @Then("{} has no booking for {}")
    public void ps_has_no_booking_for_p(String session, String participant) {
        Session s = at(session);
        ParticipantId p = at(participant);
        assertThat(s.bookings()).noneMatch(b -> b.id().participant().equals(p));
    }
}
