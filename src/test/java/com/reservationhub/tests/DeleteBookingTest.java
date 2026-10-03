package com.reservationhub.tests;

import com.reservationhub.model.Booking;
import com.reservationhub.model.CreatedBooking;
import com.reservationhub.support.BaseApiTest;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.Map;

import static com.reservationhub.support.ApiAssertions.assertStatus;
import static com.reservationhub.support.ApiAssertions.assertStatusIn;
import static com.reservationhub.support.Groups.KNOWN_DEFECT;
import static org.assertj.core.api.Assertions.assertThat;

@Epic("Bookings API")
@Feature("Cancel booking (DELETE)")
public class DeleteBookingTest extends BaseApiTest {

    @Test(description = "Cancelling a booking removes it from lookup and search")
    @Story("Happy path")
    @Severity(SeverityLevel.CRITICAL)
    public void deleteRemovesBooking() {
        CreatedBooking existing = givenExistingBooking();
        Booking booking = existing.getBooking();

        Response response = bookings.delete(existing.getBookingid(), validToken());

        // Business outcome first; the exact status code is checked separately below.
        assertStatusIn(response, "Cancellation should succeed", 200, 201, 202, 204);
        assertStatus(bookings.get(existing.getBookingid()), 404, "A cancelled booking should not be retrievable");
        assertThat(searchBookingIds(Map.of("firstname", booking.getFirstname(), "lastname", booking.getLastname())))
                .as("A cancelled booking should not appear in search results")
                .doesNotContain(existing.getBookingid());
    }

    @Test(description = "Cancelling returns 200 or 204, not 201 Created", groups = KNOWN_DEFECT)
    @Issue("BUG-11")
    @Story("Status codes")
    @Severity(SeverityLevel.MINOR)
    public void deleteReturnsConventionalStatus() {
        CreatedBooking existing = givenExistingBooking();

        Response response = bookings.delete(existing.getBookingid(), validToken());

        assertStatusIn(response, "201 Created means a resource was created; a deletion should return 200 or 204", 200, 204);
    }
}
