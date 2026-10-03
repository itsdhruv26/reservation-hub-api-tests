package com.reservationhub.testmodules.health;

import com.reservationhub.core.BaseTest;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.Test;

import static com.reservationhub.utilities.ReservationHubConstants.SMOKE;

@Epic("Bookings API")
@Feature("Health check")
public class HealthCheckTest extends BaseTest {

    @Test(description = "Health check reports the API as up", groups = SMOKE)
    @Severity(SeverityLevel.BLOCKER)
    public void pingReportsHealthy() {
        bookingController.verifyHealthy();
    }
}
