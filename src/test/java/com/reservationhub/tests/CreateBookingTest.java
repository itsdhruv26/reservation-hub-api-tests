package com.reservationhub.tests;

import com.reservationhub.builder.BookingRequestBuilder;
import com.reservationhub.data.Payloads;
import com.reservationhub.model.Booking;
import com.reservationhub.model.CreatedBooking;
import com.reservationhub.support.BaseApiTest;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;

import static com.reservationhub.support.ApiAssertions.assertMatchesSchema;
import static com.reservationhub.support.ApiAssertions.assertStatus;
import static com.reservationhub.support.Groups.KNOWN_DEFECT;
import static com.reservationhub.support.Groups.SMOKE;
import static org.assertj.core.api.Assertions.assertThat;

@Epic("Bookings API")
@Feature("Create booking")
public class CreateBookingTest extends BaseApiTest {

    @Test(description = "Creating a valid booking returns its id and echoes the stored booking", groups = SMOKE)
    @Story("Happy path")
    @Severity(SeverityLevel.BLOCKER)
    public void createReturnsIdAndBooking() {
        Booking request = BookingRequestBuilder.aBooking().build();

        Response response = bookings.create(request);
        cleanup.trackIfCreated(response);

        assertStatus(response, 200, "A valid booking should be accepted");
        assertMatchesSchema(response, "booking-created.json");
        assertThat(response.as(CreatedBooking.class).getBooking())
                .as("The response should echo exactly what was booked")
                .isEqualTo(request);
    }

    @Test(description = "A created booking is persisted and retrievable by id")
    @Story("Happy path")
    @Severity(SeverityLevel.CRITICAL)
    public void createdBookingIsPersisted() {
        CreatedBooking created = givenExistingBooking();

        Response response = bookings.get(created.getBookingid());

        assertStatus(response, 200, "A just-created booking should be retrievable");
        assertMatchesSchema(response, "booking.json");
        assertThat(response.as(Booking.class)).isEqualTo(created.getBooking());
    }

    @Test(description = "additionalneeds is optional")
    @Story("Optional fields")
    @Severity(SeverityLevel.NORMAL)
    public void additionalNeedsIsOptional() {
        Booking request = BookingRequestBuilder.aBooking().additionalneeds(null).build();

        Response response = bookings.create(request);
        cleanup.trackIfCreated(response);

        assertStatus(response, 200, "A booking without the optional additionalneeds field should be accepted");
        assertThat(response.as(CreatedBooking.class).getBooking()).isEqualTo(request);
    }

    @DataProvider
    public static Object[][] standardAcceptHeaders() {
        return new Object[][]{
                {"application/json, text/plain, */*"},
                {"application/json;q=0.9"}};
    }

    @Test(description = "Create works with standard Accept headers that allow JSON",
            dataProvider = "standardAcceptHeaders", groups = KNOWN_DEFECT)
    @Issue("BUG-12")
    @Story("Content negotiation")
    @Severity(SeverityLevel.NORMAL)
    public void createAcceptsStandardAcceptHeaders(String accept) {
        Response response = bookings.create(BookingRequestBuilder.aBooking().build(), accept);
        cleanup.trackIfCreated(response);

        // These are what common HTTP clients send by default (axios, browsers, REST Assured).
        assertStatus(response, 200, "Any Accept header that admits application/json should get a JSON response");
    }

    @Test(description = "A decimal total price is stored exactly, not truncated", groups = KNOWN_DEFECT)
    @Issue("BUG-05")
    @Story("Price handling")
    @Severity(SeverityLevel.CRITICAL)
    public void decimalPriceIsPreserved() {
        Response response = bookings.createRaw(Payloads.with(BookingRequestBuilder.aBooking().build(), "totalprice", 149.99));
        cleanup.trackIfCreated(response);

        assertStatus(response, 200, "A booking priced 149.99 should be accepted");
        // Read with jsonPath, not the Booking POJO: its Integer totalprice would hide the decimals we are checking for.
        assertThat(response.jsonPath().getDouble("booking.totalprice"))
                .as("Stored price must equal the price the partner sent (no silent rounding of money)")
                .isEqualTo(149.99);
    }
}
