package com.reservationhub.testmodules.booking;

import com.reservationhub.requestbuilder.RequestBuilderBooking;
import com.reservationhub.core.BaseTest;
import com.reservationhub.core.tokenmanager.BookingTokenManager;
import com.reservationhub.dataprovider.ReservationHubDataProvider;
import com.reservationhub.pojo.booking.BookingPayload;
import com.reservationhub.pojo.createbooking.response.CreateBookingRespPayload;
import com.reservationhub.utilities.Named;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.function.UnaryOperator;

import static com.reservationhub.utilities.ReservationHubConstants.BOOKING_SCHEMA;
import static com.reservationhub.utilities.ReservationHubConstants.KNOWN_DEFECT;
import static common.core.utils.Assertions.assertMatchesSchema;
import static common.core.utils.Assertions.assertStatusCode;
import static org.assertj.core.api.Assertions.assertThat;

@Epic("Bookings API")
@Feature("Update booking (PUT)")
public class UpdateBookingTest extends BaseTest {

    @Test(description = "PUT replaces every field and the change is persisted")
    @Story("Happy path")
    @Severity(SeverityLevel.CRITICAL)
    public void putReplacesBooking() {
        CreateBookingRespPayload existing = bookingController.givenExistingBooking();
        BookingPayload replacement = RequestBuilderBooking.genReplacementFor(existing.getBooking());

        Response response = bookingController.updateBooking(existing.getBookingid(), replacement, BookingTokenManager.getTokenAuth());

        assertStatusCode(response, 200, "A valid full update should succeed");
        assertMatchesSchema(response, BOOKING_SCHEMA);
        assertThat(response.as(BookingPayload.class)).as("Response should return the updated booking").isEqualTo(replacement);
        assertThat(bookingController.fetchBooking(existing.getBookingid())).as("Update should be persisted").isEqualTo(replacement);
    }

    @Test(description = "PUT without the optional additionalneeds clears it", groups = KNOWN_DEFECT)
    @Issue("BUG-08")
    @Story("Full replacement semantics")
    @Severity(SeverityLevel.NORMAL)
    public void putWithoutOptionalFieldClearsIt() {
        CreateBookingRespPayload existing = bookingController.givenExistingBooking(
                RequestBuilderBooking.aBooking().additionalneeds("Breakfast").build());
        BookingPayload replacement = RequestBuilderBooking.from(existing.getBooking()).additionalneeds(null).build();

        assertStatusCode(bookingController.updateBooking(existing.getBookingid(), replacement, BookingTokenManager.getTokenAuth()),
                200, "Full update should succeed");

        assertThat(bookingController.fetchBooking(existing.getBookingid()).getAdditionalneeds())
                .as("PUT is a full replacement: a field left out of the request must not survive")
                .isNull();
    }

    @Test(description = "PUT with invalid data is rejected and the booking is unchanged",
            dataProvider = "invalid_replacements", dataProviderClass = ReservationHubDataProvider.class,
            groups = KNOWN_DEFECT)
    @Issue("BUG-01")
    @Story("Validation on update")
    @Severity(SeverityLevel.CRITICAL)
    public void invalidPutIsRejected(Named<UnaryOperator<BookingPayload>> corruption) {
        CreateBookingRespPayload existing = bookingController.givenExistingBooking();

        Response response = bookingController.updateBooking(existing.getBookingid(),
                corruption.value().apply(existing.getBooking()), BookingTokenManager.getTokenAuth());

        assertStatusCode(response, 400, "An update must be validated like a create");
        assertThat(bookingController.fetchBooking(existing.getBookingid())).as("A rejected update must not change the booking")
                .isEqualTo(existing.getBooking());
    }

    @Test(description = "PUT with an incomplete body is rejected and the booking is unchanged")
    @Story("Validation on update")
    @Severity(SeverityLevel.NORMAL)
    public void incompletePutIsRejected() {
        CreateBookingRespPayload existing = bookingController.givenExistingBooking();

        Response response = bookingController.updateBooking(existing.getBookingid(),
                Map.of("firstname", "OnlyName"), BookingTokenManager.getTokenAuth());

        assertStatusCode(response, 400, "PUT requires the full booking; partial changes belong in PATCH");
        assertThat(bookingController.fetchBooking(existing.getBookingid())).isEqualTo(existing.getBooking());
    }
}
