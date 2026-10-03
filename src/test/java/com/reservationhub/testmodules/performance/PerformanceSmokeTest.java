package com.reservationhub.testmodules.performance;

import com.reservationhub.core.BaseTest;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.Test;

import static com.reservationhub.utilities.ReservationHubConstants.PERF;

@Epic("Bookings API")
@Feature("Performance smoke")
public class PerformanceSmokeTest extends BaseTest {

    @Test(description = "Concurrent creates all succeed, get distinct ids and keep their own data", groups = PERF)
    @Severity(SeverityLevel.NORMAL)
    @Description("Not a load test: a quick check that a burst of parallel partner requests doesn't fail, "
            + "collide on ids, or mix up data, plus an indicative latency figure.")
    public void concurrentCreates() {
        bookingController.verifyConcurrentCreates();
    }
}
