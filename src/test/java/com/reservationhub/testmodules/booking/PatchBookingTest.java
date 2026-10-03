package com.reservationhub.testmodules.booking;

import com.reservationhub.requestbuilder.RequestBuilderBooking;
import com.reservationhub.core.BaseTest;
import com.reservationhub.core.tokenmanager.BookingTokenManager;
import com.reservationhub.dataprovider.ReservationHubDataProvider;
import com.reservationhub.pojo.booking.BookingDates;
import com.reservationhub.pojo.booking.BookingPayload;
import com.reservationhub.pojo.createbooking.response.CreateBookingRespPayload;
import com.reservationhub.pojo.patchbooking.request.PatchBookingReqPayload;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.time.LocalDate;

import static com.reservationhub.utilities.ReservationHubConstants.KNOWN_DEFECT;
import static common.core.utils.Assertions.assertStatusCode;
import static org.assertj.core.api.Assertions.assertThat;

@Epic("Bookings API")
@Feature("Amend booking (PATCH)")
public class PatchBookingTest extends BaseTest {

    @Test(description = "PATCH changes only the fields sent and leaves the rest intact")
    @Story("Happy path")
    @Severity(SeverityLevel.CRITICAL)
    public void patchChangesOnlyGivenFields() {
        CreateBookingRespPayload existing = bookingController.givenExistingBooking();
        int newPrice = existing.getBooking().getTotalprice() + 25;
        BookingPayload expected = RequestBuilderBooking.from(existing.getBooking()).firstname("Amended").totalprice(newPrice).build();

        Response response = bookingController.patchBooking(existing.getBookingid(),
                PatchBookingReqPayload.builder().firstname("Amended").totalprice(newPrice).build(), BookingTokenManager.getTokenAuth());

        assertStatusCode(response, 200, "A valid partial update should succeed");
        assertThat(response.as(BookingPayload.class)).isEqualTo(expected);
        assertThat(bookingController.fetchBooking(existing.getBookingid())).as("Only the patched fields should change").isEqualTo(expected);
    }

    @Test(description = "Extending a stay (PATCH check-out only) keeps the check-in date", groups = KNOWN_DEFECT)
    @Issue("BUG-03")
    @Story("Nested fields")
    @Severity(SeverityLevel.CRITICAL)
    public void patchingCheckoutPreservesCheckin() {
        CreateBookingRespPayload existing = bookingController.givenExistingBooking();
        BookingDates original = existing.getBooking().getBookingdates();
        String extendedCheckout = LocalDate.parse(original.getCheckout()).plusDays(2).toString();

        Response response = bookingController.patchBooking(existing.getBookingid(),
                PatchBookingReqPayload.builder().bookingdates(BookingDates.builder().checkout(extendedCheckout).build()).build(),
                BookingTokenManager.getTokenAuth());

        assertStatusCode(response, 200, "Extending a stay should succeed");
        assertThat(bookingController.fetchBooking(existing.getBookingid()).getBookingdates())
                .as("Only check-out was sent, so check-in must be untouched")
                .isEqualTo(new BookingDates(original.getCheckin(), extendedCheckout));
    }

    @Test(description = "PATCH with invalid data is rejected and the booking is unchanged",
            dataProvider = "invalid_patches", dataProviderClass = ReservationHubDataProvider.class,
            groups = KNOWN_DEFECT)
    @Issue("BUG-01")
    @Story("Validation on amend")
    @Severity(SeverityLevel.CRITICAL)
    public void invalidPatchIsRejected(String scenario, PatchBookingReqPayload patch) {
        CreateBookingRespPayload existing = bookingController.givenExistingBooking();

        Response response = bookingController.patchBooking(existing.getBookingid(), patch, BookingTokenManager.getTokenAuth());

        assertStatusCode(response, 400, "An amendment must be validated like a create");
        assertThat(bookingController.fetchBooking(existing.getBookingid())).as("A rejected amendment must not change the booking")
                .isEqualTo(existing.getBooking());
    }
}
