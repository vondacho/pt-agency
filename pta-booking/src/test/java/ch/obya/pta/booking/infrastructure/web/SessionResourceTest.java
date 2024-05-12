package ch.obya.pta.booking.infrastructure.web;

/*-
 * #%L
 * pta-booking
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2024 obya
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import ch.obya.pta.booking.application.BookingService;
import ch.obya.pta.booking.domain.util.Samples;
import ch.obya.pta.booking.domain.vo.BookingId;
import ch.obya.pta.booking.domain.vo.SessionId;
import ch.obya.pta.common.domain.util.CommonProblem;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static ch.obya.pta.booking.domain.util.Samples.onePrivateSession;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@TestHTTPEndpoint(SessionResource.class)
@QuarkusTest
public class SessionResourceTest {

    @InjectMock
    BookingService bookingService;

    @Test
    void look_up_non_existing_session_resource_should_fail_with_404() {
        var session = SessionId.create();
        when(bookingService.findOne(any())).thenReturn(Uni.createFrom()
                .failure(CommonProblem.EntityNotFound.toException("Session", session.id()).get()));
        given()
                .when()
                .get("/{id}", Map.of("id", session.id()))
                .then()
                .log().all()
                .statusCode(404)
                .body("detail", equalTo("Session %s does not exist.".formatted(session.id())));
    }

    @Test
    void look_up_existing_session_resource_should_succeed_with_200() {
        var session = onePrivateSession.get();
        when(bookingService.findOne(session.id())).thenReturn(Uni.createFrom().item(session));
        given()
                .when()
                .get("/{id}", Map.of("id", session.id().id()))
                .then()
                .log().all()
                .statusCode(200)
                .body("id", equalTo(session.id().id().toString()));
    }

    @Test
    void booking_session_should_should_succeed_with_201() {
        var session = onePrivateSession.get();
        var participant = Samples.oneParticipant.get();

        when(bookingService.book(session.id(), participant)).thenReturn(
                Uni.createFrom().item(new BookingId(session.id(), participant)));

        given()
                .when()
                .post("/{id}/bookings/{participant}", session.id().id(), participant.id())
                .then()
                .log().all()
                .statusCode(201)
                .body(
                        "session.id", equalTo(session.id().id().toString()),
                        "participant.id", equalTo(participant.id().toString()));
    }

    @Test
    void cancel_booking_should_should_succeed_with_204() {
        var session = Samples.oneSmallGroupSession.get();
        var participant = Samples.oneParticipant.get();

        when(bookingService.cancel(session.id(), participant)).thenReturn(Uni.createFrom().voidItem());

        given()
                .when()
                .delete("/{id}/bookings/{participant}", session.id().id(), participant.id())
                .then()
                .log().all()
                .statusCode(204);
    }

    @Test
    void remove_existing_session_resource_should_succeed_with_204() {
        var session = onePrivateSession.get();
        when(bookingService.remove(session.id())).thenReturn(Uni.createFrom().voidItem());

        given()
                .when()
                .delete("/{id}", Map.of("id", session.id().id()))
                .then()
                .log().all()
                .statusCode(204);
    }

}
