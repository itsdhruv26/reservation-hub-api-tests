package com.reservationhub.support;

import com.reservationhub.http.Auth;
import com.reservationhub.builder.BookingRequestBuilder;
import com.reservationhub.config.Config;
import com.reservationhub.controller.AuthController;
import com.reservationhub.controller.BookingController;
import com.reservationhub.model.Booking;
import com.reservationhub.model.BookingId;
import com.reservationhub.model.CreatedBooking;
import io.qameta.allure.Allure;
import io.restassured.response.Response;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeSuite;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.reservationhub.support.ApiAssertions.assertStatus;

/**
 * Common fixtures. Every test creates the data it needs and {@link #cleanUp()} removes it afterwards,
 * so no test relies on the sandbox's seed data or on another test having run first.
 *
 * TestNG uses one instance of a test class for all its methods (JUnit makes a new one per method), so
 * per-test state lives in {@link BookingCleanup}, which is emptied after every method. Running methods
 * of the same class in parallel would need that state made per-thread.
 */
public abstract class BaseApiTest {

    protected final BookingController bookings = new BookingController();
    protected final AuthController authController = new AuthController();

    protected final BookingCleanup cleanup = new BookingCleanup();

    /** Runs once before the whole suite: waits out the sandbox's cold start, or skips every test if it never wakes. */
    @BeforeSuite(alwaysRun = true)
    public void waitForApi() {
        ApiReadyCheck.ensureApiIsUp();
    }

    /** alwaysRun: clean up after failed tests too, and when running a group subset with -Dgroups. */
    @AfterMethod(alwaysRun = true)
    public void cleanUp() {
        cleanup.deleteAll();
    }

    protected CreatedBooking givenExistingBooking() {
        return givenExistingBooking(BookingRequestBuilder.aBooking().build());
    }

    /**
     * Setup failures throw IllegalStateException rather than an assertion error, so Allure marks the test
     * "broken" (couldn't reach a verdict) instead of "failed" (product defect).
     */
    protected CreatedBooking givenExistingBooking(Booking booking) {
        return Allure.step("Given an existing booking", () -> {
            Response response = bookings.create(booking);
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Precondition failed: could not create booking. HTTP "
                        + response.statusCode() + ", body: " + response.asString());
            }
            CreatedBooking created = response.as(CreatedBooking.class);
            cleanup.track(created.getBookingid());
            return created;
        });
    }

    /** Reads a booking back from the API: the source of truth for "was the change actually persisted?". */
    protected Booking fetchBooking(int id) {
        return Allure.step("Read booking " + id + " back from the API", () -> {
            Response response = bookings.get(id);
            assertStatus(response, 200, "Booking " + id + " should be retrievable");
            return response.as(Booking.class);
        });
    }

    /** Ids returned by GET /booking for the given filters, e.g. {@code Map.of("firstname", "Jim")}. */
    protected List<Integer> searchBookingIds(Map<String, ?> filters) {
        Response response = bookings.list(filters);
        assertStatus(response, 200, "Search with filters " + filters + " should succeed");
        return Arrays.stream(response.as(BookingId[].class)).map(BookingId::getBookingid).toList();
    }

    protected Auth validToken() {
        return Auth.token(authController.validToken());
    }

    protected static Auth validBasicAuth() {
        return Auth.basic(Config.username(), Config.password());
    }
}
