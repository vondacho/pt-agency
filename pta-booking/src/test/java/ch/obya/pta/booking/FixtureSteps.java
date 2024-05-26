package ch.obya.pta.booking;

import ch.obya.pta.booking.domain.util.Samples;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;

import static ch.obya.pta.booking.TestContext.set;

public class FixtureSteps {

    @Given("one participant {} with one yearly subscription {}")
    public void one_participant_p_with_one_yearly_subscription_su(String p, String su) {
        set(p, Samples.oneParticipant.get());
        set(su, Samples.oneYearlySubscription.get());
    }

    @And("one private session {}")
    public void one_private_session_ps(String ps) {
        set(ps, Samples.onePrivateSession.get());
    }

    @And("one small group session {}")
    public void one_small_group_session_ps(String ps) {
        set(ps, Samples.oneSmallGroupSession.get());
    }
}
