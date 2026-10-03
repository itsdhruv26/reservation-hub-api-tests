package com.reservationhub.utilities;

public class ReservationHubConstants {

    /*
     * TestNG groups used for selecting subsets of the suite.
     *   smoke:        minimal "is the core journey alive" set; fast gate on every build.
     *   known-defect: currently fails because of a logged bug in BUGS.md. Excluding this group
     *                 (-DexcludedGroups=known-defect) gives a green gate that still catches new regressions
     *                 while the known bugs are being fixed.
     *   perf:         concurrency/latency smoke check against a shared sandbox; indicative, not a benchmark.
     */
    public static final String SMOKE = "smoke";
    public static final String KNOWN_DEFECT = "known-defect";
    public static final String PERF = "perf";

    // JSON schemas under src/main/resources/schemas
    public static final String AUTH_TOKEN_SCHEMA = "auth-token.json";
    public static final String BOOKING_SCHEMA = "booking.json";
    public static final String BOOKING_CREATED_SCHEMA = "booking-created.json";
    public static final String BOOKING_ID_LIST_SCHEMA = "booking-id-list.json";

    // Documented status of a healthy GET /ping
    public static final int PING_HEALTHY_STATUS = 201;
    // Statuses any successful cancellation may return; the exact code is checked separately (BUG-11)
    public static final Integer[] DELETE_SUCCESS_STATUSES = {200, 201, 202, 204};
}
