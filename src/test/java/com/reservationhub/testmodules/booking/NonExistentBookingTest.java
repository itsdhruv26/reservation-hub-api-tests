package com.reservationhub.testmodules.booking;

import com.reservationhub.core.BaseTest;
import com.reservationhub.dataprovider.BookingWriteCall;
import com.reservationhub.dataprovider.ReservationHubDataProvider;
import com.reservationhub.utilities.Named;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.testng.annotations.Test;

import static com.reservationhub.utilities.ReservationHubConstants.KNOWN_DEFECT;

/**
 * Behaviour for ids that don't exist. Rather than guessing an id that "probably" doesn't exist on a
 * shared instance, each scenario creates a booking and deletes it, so the id is known to be gone.
 */
@Epic("Bookings API")
@Feature("Not-found handling")
public class NonExistentBookingTest extends BaseTest {

    @Test(description = "Reading a deleted booking returns 404")
    @Story("Read")
    @Severity(SeverityLevel.NORMAL)
    public void getDeletedBookingReturns404() {
        bookingController.verifyDeletedBookingNotFound();
    }

    @Test(description = "A non-numeric id returns 404")
    @Story("Read")
    @Severity(SeverityLevel.MINOR)
    public void nonNumericIdReturns404() {
        bookingController.verifyNonNumericIdNotFound();
    }

    @Test(description = "Writing to a deleted booking returns 404",
            dataProvider = "write_calls", dataProviderClass = ReservationHubDataProvider.class,
            groups = KNOWN_DEFECT)
    @Issue("BUG-10")
    @Story("Write")
    @Severity(SeverityLevel.NORMAL)
    public void writeToDeletedBookingReturns404(Named<BookingWriteCall> call) {
        bookingController.verifyWriteToDeletedBookingNotFound(call.value());
    }
}
