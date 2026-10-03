package com.reservationhub.testmodules.booking;

import com.reservationhub.core.BaseTest;
import com.reservationhub.core.tokenmanager.BookingTokenManager;
import com.reservationhub.pojo.booking.BookingPayload;
import com.reservationhub.pojo.createbooking.response.CreateBookingRespPayload;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.Map;

import static com.reservationhub.utilities.ReservationHubConstants.DELETE_SUCCESS_STATUSES;
import static com.reservationhub.utilities.ReservationHubConstants.KNOWN_DEFECT;
import static common.core.utils.Assertions.assertStatusCode;
import static common.core.utils.Assertions.assertStatusCodeIn;
import static org.assertj.core.api.Assertions.assertThat;

@Epic("Bookings API")
@Feature("Cancel booking (DELETE)")
public class DeleteBookingTest extends BaseTest {

    @Test(description = "Cancelling a booking removes it from lookup and search")
    @Story("Happy path")
    @Severity(SeverityLevel.CRITICAL)
    public void deleteRemovesBooking() {
        CreateBookingRespPayload existing = bookingController.givenExistingBooking();
        BookingPayload booking = existing.getBooking();

        Response response = bookingController.deleteBooking(existing.getBookingid(), BookingTokenManager.getTokenAuth());

        // Business outcome first; the exact status code is checked separately below.
        assertStatusCodeIn(response, "Cancellation should succeed", DELETE_SUCCESS_STATUSES);
        assertStatusCode(bookingController.getBooking(existing.getBookingid()), 404, "A cancelled booking should not be retrievable");
        assertThat(bookingController.searchBookingIds(Map.of("firstname", booking.getFirstname(), "lastname", booking.getLastname())))
                .as("A cancelled booking should not appear in search results")
                .doesNotContain(existing.getBookingid());
    }

    @Test(description = "Cancelling returns 200 or 204, not 201 Created", groups = KNOWN_DEFECT)
    @Issue("BUG-11")
    @Story("Status codes")
    @Severity(SeverityLevel.MINOR)
    public void deleteReturnsConventionalStatus() {
        CreateBookingRespPayload existing = bookingController.givenExistingBooking();

        Response response = bookingController.deleteBooking(existing.getBookingid(), BookingTokenManager.getTokenAuth());

        assertStatusCodeIn(response, "201 Created means a resource was created; a deletion should return 200 or 204", 200, 204);
    }
}
