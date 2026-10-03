package com.reservationhub.testmodules.booking;

import com.reservationhub.core.BaseTest;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.Test;

import static com.reservationhub.utilities.ReservationHubConstants.SMOKE;

@Epic("Bookings API")
@Feature("End-to-end journey")
public class BookingLifecycleTest extends BaseTest {

    @Test(description = "A partner can create, read, amend and cancel a booking", groups = SMOKE)
    @Severity(SeverityLevel.BLOCKER)
    @Description("The core partner journey in one flow. If this fails, partners cannot transact; treat it as a release blocker.")
    public void fullBookingLifecycle() {
        bookingController.verifyFullBookingLifecycle();
    }
}
