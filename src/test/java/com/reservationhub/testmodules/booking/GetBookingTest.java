package com.reservationhub.testmodules.booking;

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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.reservationhub.utilities.ReservationHubConstants.BOOKING_ID_LIST_SCHEMA;
import static com.reservationhub.utilities.ReservationHubConstants.KNOWN_DEFECT;
import static common.core.utils.Assertions.assertMatchesSchema;
import static common.core.utils.Assertions.assertStatusCode;
import static org.assertj.core.api.Assertions.assertThat;

@Epic("Bookings API")
@Feature("Search bookings")
public class GetBookingTest extends BaseTest {

    @Test(description = "Listing bookings returns ids in the documented shape")
    @Story("List")
    @Severity(SeverityLevel.NORMAL)
    public void listMatchesContract() {
        Response response = bookingController.searchBookings(Map.of());

        assertStatusCode(response, 200, "GET /booking should list bookings");
        assertMatchesSchema(response, BOOKING_ID_LIST_SCHEMA);
    }

    @Test(description = "Filtering by first and last name finds the booking, and only when both match")
    @Story("Filter by guest name")
    @Severity(SeverityLevel.CRITICAL)
    public void nameFilterFindsBooking() {
        CreateBookingRespPayload created = bookingController.givenExistingBooking();
        BookingPayload booking = created.getBooking();

        assertThat(bookingController.searchBookingIds(Map.of("firstname", booking.getFirstname(), "lastname", booking.getLastname())))
                .as("Exact name match should return the booking")
                .containsExactly(created.getBookingid());
        assertThat(bookingController.searchBookingIds(Map.of("firstname", booking.getFirstname(), "lastname", "Someone-else")))
                .as("A different last name must not match")
                .doesNotContain(created.getBookingid());
    }

    @Test(description = "Date filters follow the documented 'on or after' semantics",
            dataProvider = "date_filter_boundaries", dataProviderClass = ReservationHubDataProvider.class)
    @Story("Filter by date")
    @Severity(SeverityLevel.CRITICAL)
    public void dateFilterBoundary(String filter, int offsetDays, boolean expectIncluded) {
        assertDateFilterBoundary(filter, offsetDays, expectIncluded);
    }

    @Test(description = "Date filters follow the documented 'on or after' semantics at the boundary",
            dataProvider = "date_filter_boundaries_known_defect", dataProviderClass = ReservationHubDataProvider.class,
            groups = KNOWN_DEFECT)
    @Issue("BUG-07")
    @Story("Filter by date")
    @Severity(SeverityLevel.CRITICAL)
    public void dateFilterBoundaryKnownDefect(String filter, int offsetDays, boolean expectIncluded) {
        assertDateFilterBoundary(filter, offsetDays, expectIncluded);
    }

    @Test(description = "A malformed date filter is rejected with 400", groups = KNOWN_DEFECT)
    @Issue("BUG-06")
    @Story("Filter by date")
    @Severity(SeverityLevel.NORMAL)
    public void malformedDateFilterIsRejected() {
        assertStatusCode(bookingController.searchBookings(Map.of("checkin", "not-a-date")), 400,
                "An unparseable filter is a client error and should get 400, not a server crash");
    }

    private void assertDateFilterBoundary(String filter, int offsetDays, boolean expectIncluded) {
        CreateBookingRespPayload created = bookingController.givenExistingBooking();
        String bookingDate = "checkin".equals(filter)
                ? created.getBooking().getBookingdates().getCheckin()
                : created.getBooking().getBookingdates().getCheckout();
        String filterDate = LocalDate.parse(bookingDate).plusDays(offsetDays).toString();

        // Narrow by the booking's unique name so the result is about our booking only.
        List<Integer> ids = bookingController.searchBookingIds(Map.of(
                "firstname", created.getBooking().getFirstname(),
                "lastname", created.getBooking().getLastname(),
                filter, filterDate));

        if (expectIncluded) {
            assertThat(ids).as("Booking with %s %s should match %s=%s", filter, bookingDate, filter, filterDate)
                    .contains(created.getBookingid());
        } else {
            assertThat(ids).as("Booking with %s %s should NOT match %s=%s", filter, bookingDate, filter, filterDate)
                    .doesNotContain(created.getBookingid());
        }
    }
}
