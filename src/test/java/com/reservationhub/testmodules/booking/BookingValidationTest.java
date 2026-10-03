package com.reservationhub.testmodules.booking;

import com.reservationhub.requestbuilder.RequestBuilderBooking;
import com.reservationhub.core.BaseTest;
import com.reservationhub.dataprovider.ReservationHubDataProvider;
import com.reservationhub.pojo.booking.BookingDates;
import com.reservationhub.pojo.booking.BookingPayload;
import com.reservationhub.pojo.createbooking.response.CreateBookingRespPayload;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import static com.reservationhub.requestbuilder.RequestBuilderBooking.FUTURE_CHECKIN;
import static com.reservationhub.utilities.ReservationHubConstants.KNOWN_DEFECT;
import static common.core.utils.Assertions.assertStatusCode;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Input validation on POST /booking. Every case asserts what a correct API must do (reject with 400),
 * whatever this API currently does. Failures here are the findings. A booking the API wrongly accepts is
 * still cleaned up, because the controller tracks every booking it sees created.
 */
@Epic("Bookings API")
@Feature("Input validation")
public class BookingValidationTest extends BaseTest {

    @Test(description = "Regression for the production incident: negative total and check-out before check-in is rejected", groups = KNOWN_DEFECT)
    @Issue("BUG-01")
    @Story("Production incident regression")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Reproduces the escalated incident exactly: one booking that is invalid on both price and dates.")
    public void incidentBookingIsRejected() {
        BookingPayload incident = RequestBuilderBooking.aBooking()
                .totalprice(-100)
                .stayFrom(FUTURE_CHECKIN, FUTURE_CHECKIN.minusDays(5))
                .build();

        assertRejected(bookingController.createBooking(incident), "negative total price and check-out before check-in");
    }

    @Test(description = "Negative total price is rejected",
            dataProvider = "negative_prices", dataProviderClass = ReservationHubDataProvider.class, groups = KNOWN_DEFECT)
    @Issue("BUG-01")
    @Story("Price rules")
    @Severity(SeverityLevel.CRITICAL)
    public void negativePriceIsRejected(int price) {
        assertRejected(bookingController.createBooking(RequestBuilderBooking.aBooking().totalprice(price).build()), "totalprice " + price);
    }

    @Test(description = "Invalid stay dates are rejected",
            dataProvider = "invalid_date_ranges", dataProviderClass = ReservationHubDataProvider.class, groups = KNOWN_DEFECT)
    @Issue("BUG-01")
    @Issue("BUG-02")
    @Story("Date rules")
    @Severity(SeverityLevel.CRITICAL)
    public void invalidDatesAreRejected(String scenario, String checkin, String checkout) {
        BookingPayload booking = RequestBuilderBooking.aBooking().bookingdates(new BookingDates(checkin, checkout)).build();

        assertRejected(bookingController.createBooking(booking), scenario);
    }

    @Test(description = "Smallest valid values are accepted: price 1, one-night stay")
    @Story("Boundaries")
    @Severity(SeverityLevel.CRITICAL)
    public void minimumValidBookingIsAccepted() {
        BookingPayload booking = RequestBuilderBooking.aBooking()
                .totalprice(1)
                .stayFrom(FUTURE_CHECKIN, FUTURE_CHECKIN.plusDays(1))
                .build();

        Response response = bookingController.createBooking(booking);

        // Guards the other side of the boundary: tightening validation must not reject legitimate bookings.
        assertStatusCode(response, 200, "The smallest legitimate booking must still be accepted");
    }

    /**
     * Zero price and same-day stays are open product questions. These cases pin today's behaviour: accepted and
     * stored exactly as sent. If product decides to forbid them, flip the expectation to 400.
     */
    @Test(description = "Open product questions (zero price, same-day stay) are accepted and stored exactly as sent",
            dataProvider = "open_product_questions", dataProviderClass = ReservationHubDataProvider.class)
    @Story("Boundaries")
    @Severity(SeverityLevel.NORMAL)
    public void openProductQuestionsArePinned(String scenario, BookingPayload booking) {
        Response response = bookingController.createBooking(booking);

        assertStatusCode(response, 200, "Booking with " + scenario + " is currently allowed (pending a product decision)");
        assertThat(bookingController.fetchBooking(response.as(CreateBookingRespPayload.class).getBookingid()))
                .as("If accepted, it must be stored exactly as sent, not silently altered")
                .isEqualTo(booking);
    }

    @Test(description = "Missing required field is rejected with 400",
            dataProvider = "required_fields", dataProviderClass = ReservationHubDataProvider.class, groups = KNOWN_DEFECT)
    @Issue("BUG-06")
    @Story("Required fields")
    @Severity(SeverityLevel.NORMAL)
    public void missingRequiredFieldIsRejected(String field) {
        assertRejected(bookingController.createBooking(
                RequestBuilderBooking.genPayloadWithout(RequestBuilderBooking.aBooking().build(), field)), "missing " + field);
    }

    @Test(description = "Wrongly typed or blank field is rejected, not coerced",
            dataProvider = "wrong_types", dataProviderClass = ReservationHubDataProvider.class, groups = KNOWN_DEFECT)
    @Issue("BUG-04")
    @Issue("BUG-06")
    @Story("Field types")
    @Severity(SeverityLevel.CRITICAL)
    public void wrongTypeIsRejected(String field, Object value) {
        assertRejected(bookingController.createBooking(
                RequestBuilderBooking.genPayloadWith(RequestBuilderBooking.aBooking().build(), field, value)), field + " = " + value);
    }

    @Test(description = "Empty or non-object body is rejected with 400",
            dataProvider = "empty_bodies", dataProviderClass = ReservationHubDataProvider.class, groups = KNOWN_DEFECT)
    @Issue("BUG-06")
    @Story("Request body")
    @Severity(SeverityLevel.NORMAL)
    public void emptyBodyIsRejected(String body) {
        assertRejected(bookingController.createBooking(body), "body " + body);
    }

    @Test(description = "Malformed JSON is rejected with 400")
    @Story("Request body")
    @Severity(SeverityLevel.NORMAL)
    public void malformedJsonIsRejected() {
        assertRejected(bookingController.createBooking("{\"firstname\": \"Jim\""), "truncated JSON");
    }

    private void assertRejected(Response response, String what) {
        assertStatusCode(response, 400, "Booking with " + what + " should be rejected as a client error");
    }
}
