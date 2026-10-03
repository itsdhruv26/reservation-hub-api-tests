package com.reservationhub.tests;

import com.reservationhub.builder.BookingRequestBuilder;
import com.reservationhub.model.Booking;
import com.reservationhub.model.CreatedBooking;
import com.reservationhub.support.BaseApiTest;
import com.reservationhub.support.Named;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;

import java.time.LocalDate;
import java.util.Map;
import java.util.function.UnaryOperator;

import static com.reservationhub.support.ApiAssertions.assertMatchesSchema;
import static com.reservationhub.support.ApiAssertions.assertStatus;
import static com.reservationhub.support.Groups.KNOWN_DEFECT;
import static com.reservationhub.support.Named.named;
import static org.assertj.core.api.Assertions.assertThat;

@Epic("Bookings API")
@Feature("Update booking (PUT)")
public class UpdateBookingTest extends BaseApiTest {

    @Test(description = "PUT replaces every field and the change is persisted")
    @Story("Happy path")
    @Severity(SeverityLevel.CRITICAL)
    public void putReplacesBooking() {
        CreatedBooking existing = givenExistingBooking();
        Booking replacement = BookingRequestBuilder.replacementFor(existing.getBooking());

        Response response = bookings.update(existing.getBookingid(), replacement, validToken());

        assertStatus(response, 200, "A valid full update should succeed");
        assertMatchesSchema(response, "booking.json");
        assertThat(response.as(Booking.class)).as("Response should return the updated booking").isEqualTo(replacement);
        assertThat(fetchBooking(existing.getBookingid())).as("Update should be persisted").isEqualTo(replacement);
    }

    @Test(description = "PUT without the optional additionalneeds clears it", groups = KNOWN_DEFECT)
    @Issue("BUG-08")
    @Story("Full replacement semantics")
    @Severity(SeverityLevel.NORMAL)
    public void putWithoutOptionalFieldClearsIt() {
        CreatedBooking existing = givenExistingBooking(BookingRequestBuilder.aBooking().additionalneeds("Breakfast").build());
        Booking replacement = BookingRequestBuilder.from(existing.getBooking()).additionalneeds(null).build();

        assertStatus(bookings.update(existing.getBookingid(), replacement, validToken()), 200, "Full update should succeed");

        assertThat(fetchBooking(existing.getBookingid()).getAdditionalneeds())
                .as("PUT is a full replacement: a field left out of the request must not survive")
                .isNull();
    }

    /** Each row: how to corrupt an otherwise valid booking, with a readable name for the report. */
    @DataProvider
    public static Object[][] invalidReplacements() {
        LocalDate checkin = LocalDate.now().plusDays(60);
        UnaryOperator<Booking> negativePrice = b -> BookingRequestBuilder.from(b).totalprice(-50).build();
        UnaryOperator<Booking> invertedDates = b -> BookingRequestBuilder.from(b).stayFrom(checkin, checkin.minusDays(2)).build();
        return new Object[][]{
                {named("negative total price", negativePrice)},
                {named("check-out before check-in", invertedDates)}};
    }

    @Test(description = "PUT with invalid data is rejected and the booking is unchanged",
            dataProvider = "invalidReplacements", groups = KNOWN_DEFECT)
    @Issue("BUG-01")
    @Story("Validation on update")
    @Severity(SeverityLevel.CRITICAL)
    public void invalidPutIsRejected(Named<UnaryOperator<Booking>> corruption) {
        CreatedBooking existing = givenExistingBooking();

        Response response = bookings.update(existing.getBookingid(), corruption.value().apply(existing.getBooking()), validToken());

        assertStatus(response, 400, "An update must be validated like a create");
        assertThat(fetchBooking(existing.getBookingid())).as("A rejected update must not change the booking")
                .isEqualTo(existing.getBooking());
    }

    @Test(description = "PUT with an incomplete body is rejected and the booking is unchanged")
    @Story("Validation on update")
    @Severity(SeverityLevel.NORMAL)
    public void incompletePutIsRejected() {
        CreatedBooking existing = givenExistingBooking();

        Response response = bookings.updateRaw(existing.getBookingid(), Map.of("firstname", "OnlyName"), validToken());

        assertStatus(response, 400, "PUT requires the full booking; partial changes belong in PATCH");
        assertThat(fetchBooking(existing.getBookingid())).isEqualTo(existing.getBooking());
    }
}
