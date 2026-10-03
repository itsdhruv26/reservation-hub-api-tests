package com.reservationhub.testmodules.booking;

import com.reservationhub.requestbuilder.RequestBuilderBooking;
import com.reservationhub.core.BaseTest;
import com.reservationhub.core.tokenmanager.BookingTokenManager;
import com.reservationhub.pojo.booking.BookingPayload;
import com.reservationhub.pojo.createbooking.response.CreateBookingRespPayload;
import com.reservationhub.pojo.patchbooking.request.PatchBookingReqPayload;
import common.core.api.Auth;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import static com.reservationhub.utilities.ReservationHubConstants.BOOKING_CREATED_SCHEMA;
import static com.reservationhub.utilities.ReservationHubConstants.DELETE_SUCCESS_STATUSES;
import static com.reservationhub.utilities.ReservationHubConstants.SMOKE;
import static common.core.utils.Assertions.assertMatchesSchema;
import static common.core.utils.Assertions.assertStatusCode;
import static common.core.utils.Assertions.assertStatusCodeIn;
import static io.qameta.allure.Allure.step;
import static org.assertj.core.api.Assertions.assertThat;

@Epic("Bookings API")
@Feature("End-to-end journey")
public class BookingLifecycleTest extends BaseTest {

    @Test(description = "A partner can create, read, amend and cancel a booking", groups = SMOKE)
    @Severity(SeverityLevel.BLOCKER)
    @Description("The core partner journey in one flow. If this fails, partners cannot transact; treat it as a release blocker.")
    public void fullBookingLifecycle() {
        Auth auth = BookingTokenManager.getTokenAuth();
        BookingPayload original = RequestBuilderBooking.aBooking().build();

        int id = step("Create booking", () -> {
            Response response = bookingController.createBooking(original);
            assertStatusCode(response, 200, "Creating a valid booking should succeed");
            assertMatchesSchema(response, BOOKING_CREATED_SCHEMA);
            CreateBookingRespPayload created = response.as(CreateBookingRespPayload.class);
            assertThat(created.getBooking()).isEqualTo(original);
            return created.getBookingid();
        });

        step("Read it back", () -> assertThat(bookingController.fetchBooking(id)).isEqualTo(original));

        BookingPayload replacement = RequestBuilderBooking.genReplacementFor(original);
        step("Replace it (PUT)", () -> {
            assertStatusCode(bookingController.updateBooking(id, replacement, auth), 200, "Full update should succeed");
            assertThat(bookingController.fetchBooking(id)).isEqualTo(replacement);
        });

        step("Amend one field (PATCH)", () -> {
            assertStatusCode(bookingController.patchBooking(id, PatchBookingReqPayload.builder().additionalneeds("Airport transfer").build(), auth),
                    200, "Partial update should succeed");
            assertThat(bookingController.fetchBooking(id))
                    .isEqualTo(RequestBuilderBooking.from(replacement).additionalneeds("Airport transfer").build());
        });

        step("Cancel it (DELETE)", () ->
                assertStatusCodeIn(bookingController.deleteBooking(id, auth), "Cancellation should succeed", DELETE_SUCCESS_STATUSES));

        step("It is gone", () ->
                assertStatusCode(bookingController.getBooking(id), 404, "A cancelled booking should no longer be retrievable"));
    }
}
