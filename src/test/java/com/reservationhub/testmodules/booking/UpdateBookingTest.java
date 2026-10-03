package com.reservationhub.testmodules.booking;

import com.reservationhub.core.BaseTest;
import com.reservationhub.dataprovider.ReservationHubDataProvider;
import com.reservationhub.pojo.booking.BookingPayload;
import com.reservationhub.utilities.Named;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.testng.annotations.Test;

import java.util.function.UnaryOperator;

import static com.reservationhub.utilities.ReservationHubConstants.KNOWN_DEFECT;

@Epic("Bookings API")
@Feature("Update booking (PUT)")
public class UpdateBookingTest extends BaseTest {

    @Test(description = "PUT replaces every field and the change is persisted")
    @Story("Happy path")
    @Severity(SeverityLevel.CRITICAL)
    public void putReplacesBooking() {
        bookingController.verifyPutReplacesBooking();
    }

    @Test(description = "PUT without the optional additionalneeds clears it", groups = KNOWN_DEFECT)
    @Issue("BUG-08")
    @Story("Full replacement semantics")
    @Severity(SeverityLevel.NORMAL)
    public void putWithoutOptionalFieldClearsIt() {
        bookingController.verifyPutWithoutAdditionalNeedsClearsIt();
    }

    @Test(description = "PUT with invalid data is rejected and the booking is unchanged",
            dataProvider = "invalid_replacements", dataProviderClass = ReservationHubDataProvider.class,
            groups = KNOWN_DEFECT)
    @Issue("BUG-01")
    @Story("Validation on update")
    @Severity(SeverityLevel.CRITICAL)
    public void invalidPutIsRejected(Named<UnaryOperator<BookingPayload>> corruption) {
        bookingController.verifyInvalidPutRejected(corruption.value());
    }

    @Test(description = "PUT with an incomplete body is rejected and the booking is unchanged")
    @Story("Validation on update")
    @Severity(SeverityLevel.NORMAL)
    public void incompletePutIsRejected() {
        bookingController.verifyIncompletePutRejected();
    }
}
