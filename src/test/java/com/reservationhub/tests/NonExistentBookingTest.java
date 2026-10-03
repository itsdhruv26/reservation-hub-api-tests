package com.reservationhub.tests;

import com.reservationhub.http.Auth;
import com.reservationhub.builder.BookingRequestBuilder;
import com.reservationhub.controller.BookingController;
import com.reservationhub.model.BookingPatch;
import com.reservationhub.support.BaseApiTest;
import com.reservationhub.support.Named;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


import static com.reservationhub.support.ApiAssertions.assertStatus;
import static com.reservationhub.support.Groups.KNOWN_DEFECT;
import static com.reservationhub.support.Named.named;

/**
 * Behaviour for ids that don't exist. Rather than guessing an id that "probably" doesn't exist on a
 * shared instance, each test creates a booking and deletes it, so the id is known to be gone.
 */
@Epic("Bookings API")
@Feature("Not-found handling")
public class NonExistentBookingTest extends BaseApiTest {

    @FunctionalInterface
    interface Call {
        Response send(BookingController client, int bookingId, Auth auth);
    }

    @Test(description = "Reading a deleted booking returns 404")
    @Story("Read")
    @Severity(SeverityLevel.NORMAL)
    public void getDeletedBookingReturns404() {
        assertStatus(bookings.get(givenDeletedBookingId()), 404, "A deleted booking should be reported as not found");
    }

    @Test(description = "A non-numeric id returns 404")
    @Story("Read")
    @Severity(SeverityLevel.MINOR)
    public void nonNumericIdReturns404() {
        assertStatus(bookings.getRaw("not-an-id"), 404, "A non-numeric id cannot match a booking");
    }

    /** Each row: the call to make, named by its HTTP method. */
    @DataProvider
    public static Object[][] writeCalls() {
        Call put = (client, id, auth) -> client.update(id, BookingRequestBuilder.aBooking().build(), auth);
        Call patch = (client, id, auth) -> client.patch(id, BookingPatch.builder().firstname("Ghost").build(), auth);
        Call delete = (client, id, auth) -> client.delete(id, auth);
        return new Object[][]{{named("PUT", put)}, {named("PATCH", patch)}, {named("DELETE", delete)}};
    }

    @Test(description = "Writing to a deleted booking returns 404", dataProvider = "writeCalls", groups = KNOWN_DEFECT)
    @Issue("BUG-10")
    @Story("Write")
    @Severity(SeverityLevel.NORMAL)
    public void writeToDeletedBookingReturns404(Named<Call> call) {
        int deletedId = givenDeletedBookingId();

        Response response = call.value().send(bookings, deletedId, validToken());

        assertStatus(response, 404, "Amending or cancelling a booking that doesn't exist should say 'not found'");
    }

    private int givenDeletedBookingId() {
        int id = givenExistingBooking().getBookingid();
        Response deletion = bookings.delete(id, validBasicAuth());
        if (deletion.statusCode() >= 300) {
            throw new IllegalStateException("Precondition failed: could not delete booking " + id
                    + ". HTTP " + deletion.statusCode());
        }
        return id;
    }
}
