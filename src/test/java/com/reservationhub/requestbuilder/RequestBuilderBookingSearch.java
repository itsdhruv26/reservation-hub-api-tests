package com.reservationhub.requestbuilder;

import com.reservationhub.pojo.booking.BookingPayload;

import java.time.LocalDate;
import java.util.Map;

/** Builds the query parameters of GET /booking. */
public final class RequestBuilderBookingSearch {

    private RequestBuilderBookingSearch() {
    }

    /** No filters: list every booking. */
    public static Map<String, String> genNoFilters() {
        return Map.of();
    }

    /** The booking's own unique first and last name, which narrows a search to that booking. */
    public static Map<String, String> genNameFilter(BookingPayload booking) {
        return genNameFilter(booking.getFirstname(), booking.getLastname());
    }

    public static Map<String, String> genNameFilter(String firstname, String lastname) {
        return Map.of("firstname", firstname, "lastname", lastname);
    }

    /**
     * A "checkin" or "checkout" filter set {@code offsetDays} from the booking's own date of that kind, plus the
     * booking's unique name so the result is about that booking only.
     */
    public static Map<String, String> genDateFilterAround(BookingPayload booking, String dateFilter, int offsetDays) {
        String bookingDate = "checkin".equals(dateFilter)
                ? booking.getBookingdates().getCheckin()
                : booking.getBookingdates().getCheckout();
        return Map.of(
                "firstname", booking.getFirstname(),
                "lastname", booking.getLastname(),
                dateFilter, LocalDate.parse(bookingDate).plusDays(offsetDays).toString());
    }

    /** Sent verbatim, so malformed dates can be probed too. */
    public static Map<String, String> genCheckinFilter(String date) {
        return Map.of("checkin", date);
    }
}
