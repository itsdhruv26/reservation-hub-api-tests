package com.reservationhub.testmodules.booking;

import com.reservationhub.core.BaseTest;
import com.reservationhub.dataprovider.ReservationHubDataProvider;
import com.reservationhub.pojo.patchbooking.request.PatchBookingReqPayload;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.testng.annotations.Test;

import static com.reservationhub.utilities.ReservationHubConstants.KNOWN_DEFECT;

@Epic("Bookings API")
@Feature("Amend booking (PATCH)")
public class PatchBookingTest extends BaseTest {

    @Test(description = "PATCH changes only the fields sent and leaves the rest intact")
    @Story("Happy path")
    @Severity(SeverityLevel.CRITICAL)
    public void patchChangesOnlyGivenFields() {
        bookingController.verifyPatchChangesOnlyGivenFields();
    }

    @Test(description = "Extending a stay (PATCH check-out only) keeps the check-in date", groups = KNOWN_DEFECT)
    @Issue("BUG-03")
    @Story("Nested fields")
    @Severity(SeverityLevel.CRITICAL)
    public void patchingCheckoutPreservesCheckin() {
        bookingController.verifyExtendingStayKeepsCheckin();
    }

    @Test(description = "PATCH with invalid data is rejected and the booking is unchanged",
            dataProvider = "invalid_patches", dataProviderClass = ReservationHubDataProvider.class,
            groups = KNOWN_DEFECT)
    @Issue("BUG-01")
    @Story("Validation on amend")
    @Severity(SeverityLevel.CRITICAL)
    public void invalidPatchIsRejected(String scenario, PatchBookingReqPayload patch) {
        bookingController.verifyInvalidPatchRejected(patch);
    }
}
