package common.core.api;

/** Every route the suite calls, relative to baseUrl. {placeholders} are filled from HttpReqObject.pathParams. */
public final class Routes {

    private Routes() {
    }

    /**
     * Auth
     **/
    public static final String AUTH = "/auth";

    /**
     * Booking APIs
     **/
    public static final String BOOKING = "/booking";
    public static final String BOOKING_BY_ID = "/booking/{id}";

    /**
     * Health check
     **/
    public static final String PING = "/ping";
}
