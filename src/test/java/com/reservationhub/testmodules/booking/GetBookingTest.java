package com.reservationhub.testmodules.booking;

import com.reservationhub.core.BaseTest;
import com.reservationhub.dataprovider.ReservationHubDataProvider;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.testng.annotations.Test;

import static com.reservationhub.utilities.ReservationHubConstants.KNOWN_DEFECT;

@Epic("Bookings API")
@Feature("Search bookings")
public class GetBookingTest extends BaseTest {

    @Test(description = "Listing bookings returns ids in the documented shape")
    @Story("List")
    @Severity(SeverityLevel.NORMAL)
    public void listMatchesContract() {
        bookingController.verifyBookingIdListMatchesContract();
    }

    @Test(description = "Filtering by first and last name finds the booking, and only when both match")
    @Story("Filter by guest name")
    @Severity(SeverityLevel.CRITICAL)
    public void nameFilterFindsBooking() {
        bookingController.verifyNameFilterFindsOnlyMatchingBooking();
    }

    @Test(description = "Date filters follow the documented 'on or after' semantics",
            dataProvider = "date_filter_boundaries", dataProviderClass = ReservationHubDataProvider.class)
    @Story("Filter by date")
    @Severity(SeverityLevel.CRITICAL)
    public void dateFilterBoundary(String filter, int offsetDays, boolean expectIncluded) {
        bookingController.verifyDateFilterBoundary(filter, offsetDays, expectIncluded);
    }

    @Test(description = "Date filters follow the documented 'on or after' semantics at the boundary",
            dataProvider = "date_filter_boundaries_known_defect", dataProviderClass = ReservationHubDataProvider.class,
            groups = KNOWN_DEFECT)
    @Issue("BUG-07")
    @Story("Filter by date")
    @Severity(SeverityLevel.CRITICAL)
    public void dateFilterBoundaryKnownDefect(String filter, int offsetDays, boolean expectIncluded) {
        bookingController.verifyDateFilterBoundary(filter, offsetDays, expectIncluded);
    }

    @Test(description = "A malformed date filter is rejected with 400", groups = KNOWN_DEFECT)
    @Issue("BUG-06")
    @Story("Filter by date")
    @Severity(SeverityLevel.NORMAL)
    public void malformedDateFilterIsRejected() {
        bookingController.verifyMalformedDateFilterRejected();
    }
}
