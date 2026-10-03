package com.reservationhub.tests;

import com.reservationhub.builder.BookingRequestBuilder;
import com.reservationhub.data.Payloads;
import com.reservationhub.model.Booking;
import com.reservationhub.model.BookingDates;
import com.reservationhub.model.CreatedBooking;
import com.reservationhub.support.BaseApiTest;
import io.qameta.allure.Description;
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

/**
 * Input validation on POST /booking. Every case asserts what a correct API must do (reject with 400),
 * whatever this API currently does. Failures here are the findings.
 */
@Epic("Bookings API")
@Feature("Input validation")
public class BookingValidationTest extends BaseApiTest {

    private static final LocalDate CHECKIN = LocalDate.now().plusDays(60);

    @Test(description = "Regression for the production incident: negative total and check-out before check-in is rejected", groups = KNOWN_DEFECT)
    @Issue("BUG-01")
    @Story("Production incident regression")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Reproduces the escalated incident exactly: one booking that is invalid on both price and dates.")
    public void incidentBookingIsRejected() {
        Booking incident = BookingRequestBuilder.aBooking()
                .totalprice(-100)
                .stayFrom(CHECKIN, CHECKIN.minusDays(5))
                .build();

        assertRejected(bookings.create(incident), "negative total price and check-out before check-in");
    }

    @DataProvider
    public static Object[][] negativePrices() {
        return new Object[][]{{-1}, {-100}};
    }

    @Test(description = "Negative total price is rejected", dataProvider = "negativePrices", groups = KNOWN_DEFECT)
    @Issue("BUG-01")
    @Story("Price rules")
    @Severity(SeverityLevel.CRITICAL)
    public void negativePriceIsRejected(int price) {
        assertRejected(bookings.create(BookingRequestBuilder.aBooking().totalprice(price).build()), "totalprice " + price);
    }

    /** Each row: scenario, check-in, check-out. */
    @DataProvider
    public static Object[][] invalidDateRanges() {
        return new Object[][]{
                {"check-out before check-in", CHECKIN.plusDays(5).toString(), CHECKIN.toString()},
                {"impossible calendar date (30 February)", "2031-02-30", "2031-03-05"},
                {"non-ISO format (MM/DD/YYYY)", "05/01/2031", "05/04/2031"},
                {"not a date at all", "not-a-date", CHECKIN.toString()}};
    }

    @Test(description = "Invalid stay dates are rejected", dataProvider = "invalidDateRanges", groups = KNOWN_DEFECT)
    @Issue("BUG-01")
    @Issue("BUG-02")
    @Story("Date rules")
    @Severity(SeverityLevel.CRITICAL)
    public void invalidDatesAreRejected(String scenario, String checkin, String checkout) {
        Booking booking = BookingRequestBuilder.aBooking().bookingdates(new BookingDates(checkin, checkout)).build();

        assertRejected(bookings.create(booking), scenario);
    }

    @Test(description = "Smallest valid values are accepted: price 1, one-night stay")
    @Story("Boundaries")
    @Severity(SeverityLevel.CRITICAL)
    public void minimumValidBookingIsAccepted() {
        Booking booking = BookingRequestBuilder.aBooking()
                .totalprice(1)
                .stayFrom(CHECKIN, CHECKIN.plusDays(1))
                .build();

        Response response = bookings.create(booking);
        cleanup.trackIfCreated(response);

        // Guards the other side of the boundary: tightening validation must not reject legitimate bookings.
        assertStatus(response, 200, "The smallest legitimate booking must still be accepted");
    }

    /**
     * Zero price (complimentary stay) and same-day check-out (day-use room) could be legitimate, so whether to
     * allow them is a product decision, not a defect (see "Observations" in BUGS.md). These cases pin today's
     * behaviour: accepted and stored exactly as sent. If product decides to forbid them, flip the expectation to 400.
     */
    @DataProvider
    public static Object[][] openProductQuestions() {
        return new Object[][]{
                {"zero total price", BookingRequestBuilder.aBooking().totalprice(0).build()},
                {"same-day check-in and check-out", BookingRequestBuilder.aBooking().stayFrom(CHECKIN, CHECKIN).build()}};
    }

    @Test(description = "Open product questions (zero price, same-day stay) are accepted and stored exactly as sent",
            dataProvider = "openProductQuestions")
    @Story("Boundaries")
    @Severity(SeverityLevel.NORMAL)
    public void openProductQuestionsArePinned(String scenario, Booking booking) {
        Response response = bookings.create(booking);
        cleanup.trackIfCreated(response);

        assertStatus(response, 200, "Booking with " + scenario + " is currently allowed (pending a product decision)");
        assertThat(fetchBooking(response.as(CreatedBooking.class).getBookingid()))
                .as("If accepted, it must be stored exactly as sent, not silently altered")
                .isEqualTo(booking);
    }

    /** Each required field in turn, using dot notation for nested fields. */
    @DataProvider
    public static Object[][] requiredFields() {
        return new Object[][]{{"firstname"}, {"lastname"}, {"totalprice"}, {"depositpaid"},
                {"bookingdates"}, {"bookingdates.checkin"}, {"bookingdates.checkout"}};
    }

    @Test(description = "Missing required field is rejected with 400", dataProvider = "requiredFields", groups = KNOWN_DEFECT)
    @Issue("BUG-06")
    @Story("Required fields")
    @Severity(SeverityLevel.NORMAL)
    public void missingRequiredFieldIsRejected(String field) {
        assertRejected(bookings.createRaw(Payloads.without(BookingRequestBuilder.aBooking().build(), field)), "missing " + field);
    }

    /** Each row: field, value of the wrong type (or blank) to put in it. */
    @DataProvider
    public static Object[][] wrongTypes() {
        return new Object[][]{
                {"totalprice", "abc"},
                {"depositpaid", "yes"},
                {"firstname", 123},
                {"firstname", ""},
                {"bookingdates", "2031-01-01"}};
    }

    @Test(description = "Wrongly typed or blank field is rejected, not coerced", dataProvider = "wrongTypes", groups = KNOWN_DEFECT)
    @Issue("BUG-04")
    @Issue("BUG-06")
    @Story("Field types")
    @Severity(SeverityLevel.CRITICAL)
    public void wrongTypeIsRejected(String field, Object value) {
        assertRejected(bookings.createRaw(Payloads.with(BookingRequestBuilder.aBooking().build(), field, value)),
                field + " = " + value);
    }

    @DataProvider
    public static Object[][] emptyBodies() {
        return new Object[][]{{"{}"}, {"[]"}, {""}};
    }

    @Test(description = "Empty or non-object body is rejected with 400", dataProvider = "emptyBodies", groups = KNOWN_DEFECT)
    @Issue("BUG-06")
    @Story("Request body")
    @Severity(SeverityLevel.NORMAL)
    public void emptyBodyIsRejected(String body) {
        assertRejected(bookings.createRaw(body), "body " + body);
    }

    @Test(description = "Malformed JSON is rejected with 400")
    @Story("Request body")
    @Severity(SeverityLevel.NORMAL)
    public void malformedJsonIsRejected() {
        assertRejected(bookings.createRaw("{\"firstname\": \"Jim\""), "truncated JSON");
    }

    private void assertRejected(Response response, String what) {
        // If the API wrongly accepts the booking, it must still be cleaned up.
        cleanup.trackIfCreated(response);
        assertStatus(response, 400, "Booking with " + what + " should be rejected as a client error");
    }
}
