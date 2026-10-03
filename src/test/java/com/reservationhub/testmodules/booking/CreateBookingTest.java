package com.reservationhub.testmodules.booking;

import com.reservationhub.requestbuilder.RequestBuilderBooking;
import com.reservationhub.core.BaseTest;
import com.reservationhub.dataprovider.ReservationHubDataProvider;
import com.reservationhub.pojo.booking.BookingPayload;
import com.reservationhub.pojo.createbooking.response.CreateBookingRespPayload;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import static com.reservationhub.utilities.ReservationHubConstants.BOOKING_CREATED_SCHEMA;
import static com.reservationhub.utilities.ReservationHubConstants.BOOKING_SCHEMA;
import static com.reservationhub.utilities.ReservationHubConstants.KNOWN_DEFECT;
import static com.reservationhub.utilities.ReservationHubConstants.SMOKE;
import static common.core.utils.Assertions.assertMatchesSchema;
import static common.core.utils.Assertions.assertStatusCode;
import static org.assertj.core.api.Assertions.assertThat;

@Epic("Bookings API")
@Feature("Create booking")
public class CreateBookingTest extends BaseTest {

    @Test(description = "Creating a valid booking returns its id and echoes the stored booking", groups = SMOKE)
    @Story("Happy path")
    @Severity(SeverityLevel.BLOCKER)
    public void createReturnsIdAndBooking() {
        BookingPayload request = RequestBuilderBooking.aBooking().build();

        Response response = bookingController.createBooking(request);

        assertStatusCode(response, 200, "A valid booking should be accepted");
        assertMatchesSchema(response, BOOKING_CREATED_SCHEMA);
        assertThat(response.as(CreateBookingRespPayload.class).getBooking())
                .as("The response should echo exactly what was booked")
                .isEqualTo(request);
    }

    @Test(description = "A created booking is persisted and retrievable by id")
    @Story("Happy path")
    @Severity(SeverityLevel.CRITICAL)
    public void createdBookingIsPersisted() {
        CreateBookingRespPayload created = bookingController.givenExistingBooking();

        Response response = bookingController.getBooking(created.getBookingid());

        assertStatusCode(response, 200, "A just-created booking should be retrievable");
        assertMatchesSchema(response, BOOKING_SCHEMA);
        assertThat(response.as(BookingPayload.class)).isEqualTo(created.getBooking());
    }

    @Test(description = "additionalneeds is optional")
    @Story("Optional fields")
    @Severity(SeverityLevel.NORMAL)
    public void additionalNeedsIsOptional() {
        BookingPayload request = RequestBuilderBooking.aBooking().additionalneeds(null).build();

        Response response = bookingController.createBooking(request);

        assertStatusCode(response, 200, "A booking without the optional additionalneeds field should be accepted");
        assertThat(response.as(CreateBookingRespPayload.class).getBooking()).isEqualTo(request);
    }

    @Test(description = "Create works with standard Accept headers that allow JSON",
            dataProvider = "standard_accept_headers", dataProviderClass = ReservationHubDataProvider.class,
            groups = KNOWN_DEFECT)
    @Issue("BUG-12")
    @Story("Content negotiation")
    @Severity(SeverityLevel.NORMAL)
    public void createAcceptsStandardAcceptHeaders(String accept) {
        Response response = bookingController.createBooking(RequestBuilderBooking.aBooking().build(), accept);

        assertStatusCode(response, 200, "Any Accept header that admits application/json should get a JSON response");
    }

    @Test(description = "A decimal total price is stored exactly, not truncated", groups = KNOWN_DEFECT)
    @Issue("BUG-05")
    @Story("Price handling")
    @Severity(SeverityLevel.CRITICAL)
    public void decimalPriceIsPreserved() {
        Response response = bookingController.createBooking(
                RequestBuilderBooking.genPayloadWith(RequestBuilderBooking.aBooking().build(), "totalprice", 149.99));

        assertStatusCode(response, 200, "A booking priced 149.99 should be accepted");
        // Read with jsonPath, not the BookingPayload POJO: its Integer totalprice would hide the decimals we are checking for.
        assertThat(response.jsonPath().getDouble("booking.totalprice"))
                .as("Stored price must equal the price the partner sent (no silent rounding of money)")
                .isEqualTo(149.99);
    }
}
