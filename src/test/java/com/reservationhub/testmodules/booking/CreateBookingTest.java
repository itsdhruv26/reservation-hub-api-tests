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
import static com.reservationhub.utilities.ReservationHubConstants.SMOKE;

@Epic("Bookings API")
@Feature("Create booking")
public class CreateBookingTest extends BaseTest {

    @Test(description = "Creating a valid booking returns its id and echoes the stored booking", groups = SMOKE)
    @Story("Happy path")
    @Severity(SeverityLevel.BLOCKER)
    public void createReturnsIdAndBooking() {
        bookingController.verifyValidBookingCreated();
    }

    @Test(description = "A created booking is persisted and retrievable by id")
    @Story("Happy path")
    @Severity(SeverityLevel.CRITICAL)
    public void createdBookingIsPersisted() {
        bookingController.verifyCreatedBookingRetrievable();
    }

    @Test(description = "additionalneeds is optional")
    @Story("Optional fields")
    @Severity(SeverityLevel.NORMAL)
    public void additionalNeedsIsOptional() {
        bookingController.verifyBookingWithoutAdditionalNeedsCreated();
    }

    @Test(description = "Create works with standard Accept headers that allow JSON",
            dataProvider = "standard_accept_headers", dataProviderClass = ReservationHubDataProvider.class,
            groups = KNOWN_DEFECT)
    @Issue("BUG-12")
    @Story("Content negotiation")
    @Severity(SeverityLevel.NORMAL)
    public void createAcceptsStandardAcceptHeaders(String accept) {
        bookingController.verifyCreateAcceptsAcceptHeader(accept);
    }

    @Test(description = "A decimal total price is stored exactly, not truncated", groups = KNOWN_DEFECT)
    @Issue("BUG-05")
    @Story("Price handling")
    @Severity(SeverityLevel.CRITICAL)
    public void decimalPriceIsPreserved() {
        bookingController.verifyDecimalPricePreserved();
    }
}
