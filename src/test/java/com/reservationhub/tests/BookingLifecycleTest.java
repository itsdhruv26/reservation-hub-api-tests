package com.reservationhub.tests;

import com.reservationhub.http.Auth;
import com.reservationhub.builder.BookingRequestBuilder;
import com.reservationhub.model.Booking;
import com.reservationhub.model.BookingPatch;
import com.reservationhub.model.CreatedBooking;
import com.reservationhub.support.BaseApiTest;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import static com.reservationhub.support.ApiAssertions.assertMatchesSchema;
import static com.reservationhub.support.ApiAssertions.assertStatus;
import static com.reservationhub.support.ApiAssertions.assertStatusIn;
import static com.reservationhub.support.Groups.SMOKE;
import static io.qameta.allure.Allure.step;
import static org.assertj.core.api.Assertions.assertThat;

@Epic("Bookings API")
@Feature("End-to-end journey")
public class BookingLifecycleTest extends BaseApiTest {

    @Test(description = "A partner can create, read, amend and cancel a booking", groups = SMOKE)
    @Severity(SeverityLevel.BLOCKER)
    @Description("The core partner journey in one flow. If this fails, partners cannot transact; treat it as a release blocker.")
    public void fullBookingLifecycle() {
        Auth auth = validToken();
        Booking original = BookingRequestBuilder.aBooking().build();

        int id = step("Create booking", () -> {
            Response response = bookings.create(original);
            assertStatus(response, 200, "Creating a valid booking should succeed");
            assertMatchesSchema(response, "booking-created.json");
            CreatedBooking created = response.as(CreatedBooking.class);
            cleanup.track(created.getBookingid());
            assertThat(created.getBooking()).isEqualTo(original);
            return created.getBookingid();
        });

        step("Read it back", () -> assertThat(fetchBooking(id)).isEqualTo(original));

        Booking replacement = BookingRequestBuilder.replacementFor(original);
        step("Replace it (PUT)", () -> {
            assertStatus(bookings.update(id, replacement, auth), 200, "Full update should succeed");
            assertThat(fetchBooking(id)).isEqualTo(replacement);
        });

        step("Amend one field (PATCH)", () -> {
            assertStatus(bookings.patch(id, BookingPatch.builder().additionalneeds("Airport transfer").build(), auth), 200,
                    "Partial update should succeed");
            assertThat(fetchBooking(id)).isEqualTo(BookingRequestBuilder.from(replacement).additionalneeds("Airport transfer").build());
        });

        step("Cancel it (DELETE)", () ->
                assertStatusIn(bookings.delete(id, auth), "Cancellation should succeed", 200, 201, 202, 204));

        step("It is gone", () ->
                assertStatus(bookings.get(id), 404, "A cancelled booking should no longer be retrievable"));
    }
}
