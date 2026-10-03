package com.reservationhub.tests;

import com.reservationhub.support.BaseApiTest;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.Test;

import static com.reservationhub.support.ApiAssertions.assertStatus;
import static com.reservationhub.support.Groups.SMOKE;

@Epic("Bookings API")
@Feature("Health check")
public class HealthCheckTest extends BaseApiTest {

    @Test(description = "Health check reports the API as up", groups = SMOKE)
    @Severity(SeverityLevel.BLOCKER)
    public void pingReportsHealthy() {
        assertStatus(bookings.ping(), 201, "GET /ping should report the service as healthy (documented as 201 Created)");
    }
}
