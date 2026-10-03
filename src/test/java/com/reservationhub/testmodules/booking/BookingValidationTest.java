package com.reservationhub.testmodules.booking;

import com.reservationhub.core.BaseTest;
import com.reservationhub.dataprovider.ReservationHubDataProvider;
import com.reservationhub.pojo.booking.BookingPayload;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.testng.annotations.Test;

import static com.reservationhub.utilities.ReservationHubConstants.KNOWN_DEFECT;

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
        bookingController.verifyIncidentBookingRejected();
    }

    @Test(description = "Negative total price is rejected",
            dataProvider = "negative_prices", dataProviderClass = ReservationHubDataProvider.class, groups = KNOWN_DEFECT)
    @Issue("BUG-01")
    @Story("Price rules")
    @Severity(SeverityLevel.CRITICAL)
    public void negativePriceIsRejected(int price) {
        bookingController.verifyNegativePriceRejected(price);
    }

    @Test(description = "Invalid stay dates are rejected",
            dataProvider = "invalid_date_ranges", dataProviderClass = ReservationHubDataProvider.class, groups = KNOWN_DEFECT)
    @Issue("BUG-01")
    @Issue("BUG-02")
    @Story("Date rules")
    @Severity(SeverityLevel.CRITICAL)
    public void invalidDatesAreRejected(String scenario, String checkin, String checkout) {
        bookingController.verifyInvalidDatesRejected(scenario, checkin, checkout);
    }

    @Test(description = "Smallest valid values are accepted: price 1, one-night stay")
    @Story("Boundaries")
    @Severity(SeverityLevel.CRITICAL)
    public void minimumValidBookingIsAccepted() {
        bookingController.verifyMinimumValidBookingAccepted();
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
        bookingController.verifyAcceptedAndStoredAsSent(scenario, booking);
    }

    @Test(description = "Missing required field is rejected with 400",
            dataProvider = "required_fields", dataProviderClass = ReservationHubDataProvider.class, groups = KNOWN_DEFECT)
    @Issue("BUG-06")
    @Story("Required fields")
    @Severity(SeverityLevel.NORMAL)
    public void missingRequiredFieldIsRejected(String field) {
        bookingController.verifyMissingFieldRejected(field);
    }

    @Test(description = "Wrongly typed or blank field is rejected, not coerced",
            dataProvider = "wrong_types", dataProviderClass = ReservationHubDataProvider.class, groups = KNOWN_DEFECT)
    @Issue("BUG-04")
    @Issue("BUG-06")
    @Story("Field types")
    @Severity(SeverityLevel.CRITICAL)
    public void wrongTypeIsRejected(String field, Object value) {
        bookingController.verifyWrongTypeRejected(field, value);
    }

    @Test(description = "Empty or non-object body is rejected with 400",
            dataProvider = "empty_bodies", dataProviderClass = ReservationHubDataProvider.class, groups = KNOWN_DEFECT)
    @Issue("BUG-06")
    @Story("Request body")
    @Severity(SeverityLevel.NORMAL)
    public void emptyBodyIsRejected(String body) {
        bookingController.verifyEmptyBodyRejected(body);
    }

    @Test(description = "Malformed JSON is rejected with 400")
    @Story("Request body")
    @Severity(SeverityLevel.NORMAL)
    public void malformedJsonIsRejected() {
        bookingController.verifyMalformedJsonRejected();
    }
}
