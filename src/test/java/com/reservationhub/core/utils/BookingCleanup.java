package com.reservationhub.core.utils;

import com.reservationhub.applicationapi.BookingFlowRequests;
import com.reservationhub.core.tokenmanager.BookingTokenManager;
import common.template.HttpReqObject;
import io.restassured.response.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Teardown: deletes every booking a test created, pass or fail, so the shared sandbox isn't polluted.
 * The controller tracks bookings as they are created; {@link com.reservationhub.core.BaseTest} calls
 * {@link #deleteAll()} from an {@code @AfterMethod(alwaysRun = true)}.
 * <p>
 * Tracking is per thread, so tests could run in parallel without deleting each other's bookings. Cleanup is
 * best effort: a booking may already be gone (deleted by the test itself or wiped by the sandbox reset), and
 * that must never fail the test.
 */
public final class BookingCleanup {

    private static final ThreadLocal<List<Integer>> CREATED_IDS = ThreadLocal.withInitial(ArrayList::new);

    private BookingCleanup() {
    }

    public static void track(int bookingId) {
        CREATED_IDS.get().add(bookingId);
    }

    /** For negative tests: if the API wrongly accepted an invalid booking, make sure it still gets removed. */
    public static void trackIfCreated(Response response) {
        if (response.statusCode() == 200 && response.contentType().contains("json")) {
            Integer id = response.jsonPath().get("bookingid");
            if (id != null) {
                track(id);
            }
        }
    }

    public static void deleteAll() {
        List<Integer> ids = CREATED_IDS.get();
        for (int id : ids) {
            try {
                BookingFlowRequests.deleteBooking(HttpReqObject.builder()
                        .pathParams(Map.of("id", id))
                        .auth(BookingTokenManager.getBasicAuth())
                        .reported(false)
                        .build());
            } catch (RuntimeException e) {
                System.err.printf("Cleanup of booking %d failed (ignored): %s%n", id, e.getMessage());
            }
        }
        CREATED_IDS.remove();
    }
}
