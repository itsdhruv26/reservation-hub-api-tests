package com.reservationhub.testmodules.booking;

import com.reservationhub.core.BaseTest;
import com.reservationhub.core.tokenmanager.BookingTokenManager;
import com.reservationhub.dataprovider.BookingWriteCall;
import com.reservationhub.dataprovider.ReservationHubDataProvider;
import com.reservationhub.utilities.Named;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import static com.reservationhub.utilities.ReservationHubConstants.KNOWN_DEFECT;
import static common.core.utils.Assertions.assertStatusCode;

/**
 * Behaviour for ids that don't exist. Rather than guessing an id that "probably" doesn't exist on a
 * shared instance, each test creates a booking and deletes it, so the id is known to be gone.
 */
@Epic("Bookings API")
@Feature("Not-found handling")
public class NonExistentBookingTest extends BaseTest {

    @Test(description = "Reading a deleted booking returns 404")
    @Story("Read")
    @Severity(SeverityLevel.NORMAL)
    public void getDeletedBookingReturns404() {
        assertStatusCode(bookingController.getBooking(bookingController.givenDeletedBookingId()), 404,
                "A deleted booking should be reported as not found");
    }

    @Test(description = "A non-numeric id returns 404")
    @Story("Read")
    @Severity(SeverityLevel.MINOR)
    public void nonNumericIdReturns404() {
        assertStatusCode(bookingController.getBooking("not-an-id"), 404, "A non-numeric id cannot match a booking");
    }

    @Test(description = "Writing to a deleted booking returns 404",
            dataProvider = "write_calls", dataProviderClass = ReservationHubDataProvider.class,
            groups = KNOWN_DEFECT)
    @Issue("BUG-10")
    @Story("Write")
    @Severity(SeverityLevel.NORMAL)
    public void writeToDeletedBookingReturns404(Named<BookingWriteCall> call) {
        int deletedId = bookingController.givenDeletedBookingId();

        Response response = call.value().send(bookingController, deletedId, BookingTokenManager.getTokenAuth());

        assertStatusCode(response, 404, "Amending or cancelling a booking that doesn't exist should say 'not found'");
    }
}
