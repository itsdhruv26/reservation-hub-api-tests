package com.reservationhub.tests;

import com.reservationhub.builder.BookingRequestBuilder;
import com.reservationhub.model.Booking;
import com.reservationhub.model.BookingDates;
import com.reservationhub.model.BookingPatch;
import com.reservationhub.model.CreatedBooking;
import com.reservationhub.support.BaseApiTest;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.time.LocalDate;

import static com.reservationhub.support.ApiAssertions.assertStatus;
import static com.reservationhub.support.Groups.KNOWN_DEFECT;
import static org.assertj.core.api.Assertions.assertThat;

@Epic("Bookings API")
@Feature("Amend booking (PATCH)")
public class PatchBookingTest extends BaseApiTest {

    @Test(description = "PATCH changes only the fields sent and leaves the rest intact")
    @Story("Happy path")
    @Severity(SeverityLevel.CRITICAL)
    public void patchChangesOnlyGivenFields() {
        CreatedBooking existing = givenExistingBooking();
        int newPrice = existing.getBooking().getTotalprice() + 25;
        Booking expected = BookingRequestBuilder.from(existing.getBooking()).firstname("Amended").totalprice(newPrice).build();

        Response response = bookings.patch(existing.getBookingid(),
                BookingPatch.builder().firstname("Amended").totalprice(newPrice).build(), validToken());

        assertStatus(response, 200, "A valid partial update should succeed");
        assertThat(response.as(Booking.class)).isEqualTo(expected);
        assertThat(fetchBooking(existing.getBookingid())).as("Only the patched fields should change").isEqualTo(expected);
    }

    @Test(description = "Extending a stay (PATCH check-out only) keeps the check-in date", groups = KNOWN_DEFECT)
    @Issue("BUG-03")
    @Story("Nested fields")
    @Severity(SeverityLevel.CRITICAL)
    public void patchingCheckoutPreservesCheckin() {
        CreatedBooking existing = givenExistingBooking();
        BookingDates original = existing.getBooking().getBookingdates();
        String extendedCheckout = LocalDate.parse(original.getCheckout()).plusDays(2).toString();

        Response response = bookings.patch(existing.getBookingid(),
                BookingPatch.builder().bookingdates(BookingDates.builder().checkout(extendedCheckout).build()).build(), validToken());

        assertStatus(response, 200, "Extending a stay should succeed");
        assertThat(fetchBooking(existing.getBookingid()).getBookingdates())
                .as("Only check-out was sent, so check-in must be untouched")
                .isEqualTo(new BookingDates(original.getCheckin(), extendedCheckout));
    }

    /** Each row: a readable label for the report, then the PATCH body to send. */
    @DataProvider
    public static Object[][] invalidPatches() {
        LocalDate checkin = LocalDate.now().plusDays(60);
        return new Object[][]{
                {"negative total price", BookingPatch.builder().totalprice(-1).build()},
                {"check-out before check-in", BookingPatch.builder()
                        .bookingdates(BookingDates.of(checkin, checkin.minusDays(3))).build()}};
    }

    @Test(description = "PATCH with invalid data is rejected and the booking is unchanged",
            dataProvider = "invalidPatches", groups = KNOWN_DEFECT)
    @Issue("BUG-01")
    @Story("Validation on amend")
    @Severity(SeverityLevel.CRITICAL)
    public void invalidPatchIsRejected(String scenario, BookingPatch patch) {
        CreatedBooking existing = givenExistingBooking();

        Response response = bookings.patch(existing.getBookingid(), patch, validToken());

        assertStatus(response, 400, "An amendment must be validated like a create");
        assertThat(fetchBooking(existing.getBookingid())).as("A rejected amendment must not change the booking")
                .isEqualTo(existing.getBooking());
    }
}
