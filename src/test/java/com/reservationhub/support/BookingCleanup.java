package com.reservationhub.support;

import com.reservationhub.http.Auth;
import com.reservationhub.http.RequestSpecs;
import com.reservationhub.config.Config;
import com.reservationhub.controller.BookingController;
import io.restassured.response.Response;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Teardown: deletes every booking a test created, pass or fail, so the shared sandbox isn't
 * polluted. {@link BaseApiTest} calls {@link #deleteAll()} from an {@code @AfterMethod(alwaysRun = true)}. Cleanup is best effort: a booking may already be gone (deleted by the test itself
 * or wiped by the sandbox reset), and that must never fail the test.
 */
public final class BookingCleanup {

    private final BookingController client = new BookingController(RequestSpecs.unreported());
    private final List<Integer> createdIds = new CopyOnWriteArrayList<>();

    public void track(int bookingId) {
        createdIds.add(bookingId);
    }

    /** For negative tests: if the API wrongly accepted an invalid booking, make sure it still gets removed. */
    public void trackIfCreated(Response response) {
        if (response.statusCode() == 200 && response.contentType().contains("json")) {
            Integer id = response.jsonPath().get("bookingid");
            if (id != null) {
                track(id);
            }
        }
    }

    public void deleteAll() {
        Auth admin = Auth.basic(Config.username(), Config.password());
        for (int id : createdIds) {
            try {
                client.delete(id, admin);
            } catch (RuntimeException e) {
                System.err.printf("Cleanup of booking %d failed (ignored): %s%n", id, e.getMessage());
            }
        }
        createdIds.clear();
    }
}
