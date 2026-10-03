package com.reservationhub.requestbuilder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reservationhub.pojo.booking.BookingDates;
import com.reservationhub.pojo.booking.BookingPayload;
import com.reservationhub.pojo.patchbooking.request.PatchBookingReqPayload;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Builds every booking body (POST and PUT) the suite sends, so tests never assemble payloads themselves.
 * Each booking starts from a valid booking that is unique per call, so a test can always find "its" booking
 * (e.g. via name filters) on a shared instance full of other people's data, and only the fields a scenario
 * cares about are then changed:
 * <ul>
 *   <li>{@code genBooking...}: new bookings for POST /booking.</li>
 *   <li>{@code genReplacement...}: full bodies for PUT, derived from an existing booking.</li>
 *   <li>{@code genPayload...}, {@link #genIncompletePayload()}, {@link #genMalformedJson()}: deliberately
 *       broken bodies that a POJO can't represent.</li>
 *   <li>{@link #genExpectedAfterPatch}: the state a booking should be in after a PATCH.</li>
 * </ul>
 */
public final class RequestBuilderBooking {

    /** A fixed future check-in for scenarios that build their own stay dates around a known day. */
    public static final LocalDate FUTURE_CHECKIN = LocalDate.now().plusDays(60);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final BookingPayload.BookingPayloadBuilder booking;

    private RequestBuilderBooking(BookingPayload.BookingPayloadBuilder booking) {
        this.booking = booking;
    }

    // ===================== NEW BOOKINGS (POST) =====================

    /** A valid booking with random names, a future three-night stay and a price between 50 and 999. */
    public static BookingPayload genBooking() {
        return aBooking().build();
    }

    public static BookingPayload genBookingWithPrice(int totalprice) {
        return aBooking().totalprice(totalprice).build();
    }

    public static BookingPayload genBookingWithStay(LocalDate checkin, LocalDate checkout) {
        return aBooking().stayFrom(checkin, checkout).build();
    }

    /** Dates as raw strings, so malformed values (e.g. "2031-02-30", "05/01/2031") are sent verbatim. */
    public static BookingPayload genBookingWithDates(String checkin, String checkout) {
        return aBooking().bookingdates(new BookingDates(checkin, checkout)).build();
    }

    public static BookingPayload genBookingWithAdditionalNeeds(String additionalneeds) {
        return aBooking().additionalneeds(additionalneeds).build();
    }

    /** additionalneeds is optional: it is left out of the request entirely. */
    public static BookingPayload genBookingWithoutAdditionalNeeds() {
        return aBooking().additionalneeds(null).build();
    }

    /** The escalated production incident: negative total and check-out five days before check-in. */
    public static BookingPayload genIncidentBooking() {
        return aBooking().totalprice(-100).stayFrom(FUTURE_CHECKIN, FUTURE_CHECKIN.minusDays(5)).build();
    }

    /** The smallest legitimate booking: price 1 and a one-night stay. */
    public static BookingPayload genMinimumValidBooking() {
        return aBooking().totalprice(1).stayFrom(FUTURE_CHECKIN, FUTURE_CHECKIN.plusDays(1)).build();
    }

    // ===================== REPLACEMENTS (PUT) =====================

    /** A valid booking that differs from {@code original} in every field, for full-replacement (PUT) checks. */
    public static BookingPayload genReplacementFor(BookingPayload original) {
        LocalDate newCheckin = LocalDate.parse(original.getBookingdates().getCheckin()).plusDays(10);
        return aBooking()
                .totalprice(original.getTotalprice() + 100)
                .depositpaid(!original.getDepositpaid())
                .stayFrom(newCheckin, newCheckin.plusDays(5))
                .additionalneeds("Late checkout")
                .build();
    }

    public static BookingPayload genReplacementWithPrice(BookingPayload original, int totalprice) {
        return from(original).totalprice(totalprice).build();
    }

    public static BookingPayload genReplacementWithStay(BookingPayload original, LocalDate checkin, LocalDate checkout) {
        return from(original).stayFrom(checkin, checkout).build();
    }

    /** {@code original} unchanged except that the optional additionalneeds is left out. */
    public static BookingPayload genReplacementWithoutAdditionalNeeds(BookingPayload original) {
        return from(original).additionalneeds(null).build();
    }

    // ===================== EXPECTED STATE AFTER PATCH =====================

    /**
     * {@code original} with every field the patch sets overwritten and everything else kept. Nested booking
     * dates are merged field by field, so patching only the check-out keeps the check-in.
     */
    public static BookingPayload genExpectedAfterPatch(BookingPayload original, PatchBookingReqPayload patch) {
        RequestBuilderBooking expected = from(original);
        Optional.ofNullable(patch.getFirstname()).ifPresent(expected::firstname);
        Optional.ofNullable(patch.getLastname()).ifPresent(expected::lastname);
        Optional.ofNullable(patch.getTotalprice()).ifPresent(expected::totalprice);
        Optional.ofNullable(patch.getDepositpaid()).ifPresent(expected::depositpaid);
        Optional.ofNullable(patch.getAdditionalneeds()).ifPresent(expected::additionalneeds);
        Optional.ofNullable(patch.getBookingdates()).ifPresent(dates -> {
            BookingDates current = original.getBookingdates();
            expected.bookingdates(new BookingDates(
                    dates.getCheckin() != null ? dates.getCheckin() : current.getCheckin(),
                    dates.getCheckout() != null ? dates.getCheckout() : current.getCheckout()));
        });
        return expected.build();
    }

    // ===================== BROKEN BODIES =====================

    /** A valid booking as JSON with one field removed. Dot notation for nested fields, e.g. "bookingdates.checkin". */
    public static Map<String, Object> genPayloadWithout(String path) {
        Map<String, Object> payload = toMap(genBooking());
        parentOf(payload, path).remove(leafOf(path));
        return payload;
    }

    /** A valid booking as JSON with one field set to any value, typically of the wrong type. */
    public static Map<String, Object> genPayloadWith(String path, Object value) {
        Map<String, Object> payload = toMap(genBooking());
        parentOf(payload, path).put(leafOf(path), value);
        return payload;
    }

    /** Only a first name: a partial change, which belongs in PATCH, not PUT. */
    public static Map<String, Object> genIncompletePayload() {
        return Map.of("firstname", "OnlyName");
    }

    /** Truncated JSON: the closing brace is missing. */
    public static String genMalformedJson() {
        return "{\"firstname\": \"Jim\"";
    }

    // ===================== FLUENT HELPERS =====================

    private static RequestBuilderBooking aBooking() {
        LocalDate checkin = LocalDate.now().plusDays(ThreadLocalRandom.current().nextInt(30, 365));
        return new RequestBuilderBooking(BookingPayload.builder()
                .firstname("Qa" + uniqueSuffix())
                .lastname("Auto" + uniqueSuffix())
                .totalprice(ThreadLocalRandom.current().nextInt(50, 1000))
                .depositpaid(true)
                .bookingdates(BookingDates.of(checkin, checkin.plusDays(3)))
                .additionalneeds("Breakfast"));
    }

    private static RequestBuilderBooking from(BookingPayload original) {
        return new RequestBuilderBooking(original.toBuilder());
    }

    private RequestBuilderBooking firstname(String value) {
        booking.firstname(value);
        return this;
    }

    private RequestBuilderBooking lastname(String value) {
        booking.lastname(value);
        return this;
    }

    private RequestBuilderBooking totalprice(Integer value) {
        booking.totalprice(value);
        return this;
    }

    private RequestBuilderBooking depositpaid(Boolean value) {
        booking.depositpaid(value);
        return this;
    }

    private RequestBuilderBooking bookingdates(BookingDates value) {
        booking.bookingdates(value);
        return this;
    }

    private RequestBuilderBooking stayFrom(LocalDate checkin, LocalDate checkout) {
        return bookingdates(BookingDates.of(checkin, checkout));
    }

    /** Pass null to leave the optional field out of the request. */
    private RequestBuilderBooking additionalneeds(String value) {
        booking.additionalneeds(value);
        return this;
    }

    private BookingPayload build() {
        return booking.build();
    }

    /** 8 random lower-case letters (26^8 combinations): unique enough, and safe in query strings. */
    private static String uniqueSuffix() {
        StringBuilder suffix = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            suffix.append((char) ('a' + ThreadLocalRandom.current().nextInt(26)));
        }
        return suffix.toString();
    }

    private static Map<String, Object> toMap(BookingPayload booking) {
        return MAPPER.convertValue(booking, new TypeReference<LinkedHashMap<String, Object>>() {
        });
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parentOf(Map<String, Object> root, String path) {
        Map<String, Object> node = root;
        String[] parts = path.split("\\.");
        for (int i = 0; i < parts.length - 1; i++) {
            node = (Map<String, Object>) node.get(parts[i]);
        }
        return node;
    }

    private static String leafOf(String path) {
        return path.substring(path.lastIndexOf('.') + 1);
    }
}
