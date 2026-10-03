package com.reservationhub.tests;

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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.reservationhub.support.ApiAssertions.assertMatchesSchema;
import static com.reservationhub.support.ApiAssertions.assertStatus;
import static com.reservationhub.support.Groups.KNOWN_DEFECT;
import static org.assertj.core.api.Assertions.assertThat;

@Epic("Bookings API")
@Feature("Search bookings")
public class GetBookingTest extends BaseApiTest {

    @Test(description = "Listing bookings returns ids in the documented shape")
    @Story("List")
    @Severity(SeverityLevel.NORMAL)
    public void listMatchesContract() {
        Response response = bookings.list(Map.of());

        assertStatus(response, 200, "GET /booking should list bookings");
        assertMatchesSchema(response, "booking-id-list.json");
    }

    @Test(description = "Filtering by first and last name finds the booking, and only when both match")
    @Story("Filter by guest name")
    @Severity(SeverityLevel.CRITICAL)
    public void nameFilterFindsBooking() {
        CreatedBooking created = givenExistingBooking();
        Booking booking = created.getBooking();

        assertThat(searchBookingIds(Map.of("firstname", booking.getFirstname(), "lastname", booking.getLastname())))
                .as("Exact name match should return the booking")
                .containsExactly(created.getBookingid());
        assertThat(searchBookingIds(Map.of("firstname", booking.getFirstname(), "lastname", "Someone-else")))
                .as("A different last name must not match")
                .doesNotContain(created.getBookingid());
    }

    /**
     * The docs say: checkin returns bookings with check-in >= the given date, and checkout returns
     * bookings with check-out >= the given date. Each case puts the filter date just before, on or after
     * the booking's own date to probe the boundary.
     */
    @DataProvider
    public static Object[][] dateFilterBoundaries() {
        return new Object[][]{
                // filter, filter date relative to the booking's date (days), expected to be included
                {"checkin", -1, true},
                {"checkin", 0, true},
                {"checkin", 1, false},
                {"checkout", -1, true},
                {"checkout", 0, true},
                {"checkout", 1, false}};
    }

    @Test(description = "Date filters follow the documented 'on or after' semantics",
            dataProvider = "dateFilterBoundaries", groups = KNOWN_DEFECT)
    @Issue("BUG-07")
    @Story("Filter by date")
    @Severity(SeverityLevel.CRITICAL)
    public void dateFilterBoundary(String filter, int offsetDays, boolean expectIncluded) {
        CreatedBooking created = givenExistingBooking();
        String bookingDate = "checkin".equals(filter)
                ? created.getBooking().getBookingdates().getCheckin()
                : created.getBooking().getBookingdates().getCheckout();
        String filterDate = LocalDate.parse(bookingDate).plusDays(offsetDays).toString();

        // Narrow by the booking's unique name so the result is about our booking only.
        List<Integer> ids = searchBookingIds(Map.of(
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

    @Test(description = "A malformed date filter is rejected with 400", groups = KNOWN_DEFECT)
    @Issue("BUG-06")
    @Story("Filter by date")
    @Severity(SeverityLevel.NORMAL)
    public void malformedDateFilterIsRejected() {
        assertStatus(bookings.list(Map.of("checkin", "not-a-date")), 400,
                "An unparseable filter is a client error and should get 400, not a server crash");
    }
}
