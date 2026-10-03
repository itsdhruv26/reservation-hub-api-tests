package com.reservationhub.testmodules.booking;

import com.reservationhub.core.BaseTest;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.testng.annotations.Test;

import static com.reservationhub.utilities.ReservationHubConstants.KNOWN_DEFECT;

@Epic("Bookings API")
@Feature("Cancel booking (DELETE)")
public class DeleteBookingTest extends BaseTest {

    @Test(description = "Cancelling a booking removes it from lookup and search")
    @Story("Happy path")
    @Severity(SeverityLevel.CRITICAL)
    public void deleteRemovesBooking() {
        bookingController.verifyCancelRemovesBooking();
    }

    @Test(description = "Cancelling returns 200 or 204, not 201 Created", groups = KNOWN_DEFECT)
    @Issue("BUG-11")
    @Story("Status codes")
    @Severity(SeverityLevel.MINOR)
    public void deleteReturnsConventionalStatus() {
        bookingController.verifyCancelReturnsConventionalStatus();
    }
}
