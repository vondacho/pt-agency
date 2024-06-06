package ch.obya.pta.booking;

import io.cucumber.java.en.When;

import ch.obya.pta.booking.domain.aggregate.Session;
import ch.obya.pta.booking.domain.vo.ParticipantId;
import ch.obya.pta.booking.domain.vo.Subscription;

import static ch.obya.pta.booking.TestContext.at;

public class ActionSteps {

    @When("{} books {} using {}")
    public void p_books_ps_using_su(String p, String ps, String su) {
        Session session = at(ps);
        ParticipantId participant = at(p);
        Subscription subscription = at(su);
        session.book(participant, subscription.id());
    }

    @When("{} cancels his booking for {}")
    public void p_cancels_his_booking_for_ps(String p, String ps) {
        Session session = at(ps);
        ParticipantId participant = at(p);
        session.cancel(participant);
    }
}
