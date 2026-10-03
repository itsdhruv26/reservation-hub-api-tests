package com.reservationhub.requestbuilder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reservationhub.pojo.booking.BookingDates;
import com.reservationhub.pojo.booking.BookingPayload;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Builds booking request bodies. {@link #aBooking()} starts from a valid booking that is unique per call, so a
 * test can always find "its" booking (e.g. via name filters) on a shared instance full of other people's data,
 * and then breaks or tweaks only the fields a test cares about:
 *
 * <pre>{@code
 * BookingPayload incident = RequestBuilderBooking.aBooking().totalprice(-100).stayFrom(checkin, checkin.minusDays(5)).build();
 * }</pre>
 *
 * {@link #from(BookingPayload)} starts from an existing booking instead, which is how tests express the expected
 * state after an amendment. {@link #genPayloadWithout} and {@link #genPayloadWith} turn a valid booking into a
 * deliberately broken JSON payload.
 */
public final class RequestBuilderBooking {

    /** A fixed future check-in for tests that build their own stay dates around a known day. */
    public static final LocalDate FUTURE_CHECKIN = LocalDate.now().plusDays(60);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final BookingPayload.BookingPayloadBuilder booking;

    private RequestBuilderBooking(BookingPayload.BookingPayloadBuilder booking) {
        this.booking = booking;
    }

    /** A valid booking with random names, a future three-night stay and a price between 50 and 999. */
    public static RequestBuilderBooking aBooking() {
        LocalDate checkin = LocalDate.now().plusDays(ThreadLocalRandom.current().nextInt(30, 365));
        return new RequestBuilderBooking(BookingPayload.builder()
                .firstname("Qa" + uniqueSuffix())
                .lastname("Auto" + uniqueSuffix())
                .totalprice(ThreadLocalRandom.current().nextInt(50, 1000))
                .depositpaid(true)
                .bookingdates(BookingDates.of(checkin, checkin.plusDays(3)))
                .additionalneeds("Breakfast"));
    }

    public static RequestBuilderBooking from(BookingPayload original) {
        return new RequestBuilderBooking(original.toBuilder());
    }

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

    /** The booking as JSON with one field removed. Dot notation for nested fields, e.g. "bookingdates.checkin". */
    public static Map<String, Object> genPayloadWithout(BookingPayload booking, String path) {
        Map<String, Object> payload = toMap(booking);
        parentOf(payload, path).remove(leafOf(path));
        return payload;
    }

    /** The booking as JSON with one field set to any value, typically of the wrong type. */
    public static Map<String, Object> genPayloadWith(BookingPayload booking, String path, Object value) {
        Map<String, Object> payload = toMap(booking);
        parentOf(payload, path).put(leafOf(path), value);
        return payload;
    }

    public RequestBuilderBooking firstname(String value) {
        booking.firstname(value);
        return this;
    }

    public RequestBuilderBooking lastname(String value) {
        booking.lastname(value);
        return this;
    }

    public RequestBuilderBooking totalprice(Integer value) {
        booking.totalprice(value);
        return this;
    }

    public RequestBuilderBooking depositpaid(Boolean value) {
        booking.depositpaid(value);
        return this;
    }

    public RequestBuilderBooking bookingdates(BookingDates value) {
        booking.bookingdates(value);
        return this;
    }

    public RequestBuilderBooking stayFrom(LocalDate checkin, LocalDate checkout) {
        return bookingdates(BookingDates.of(checkin, checkout));
    }

    /** Pass null to leave the optional field out of the request. */
    public RequestBuilderBooking additionalneeds(String value) {
        booking.additionalneeds(value);
        return this;
    }

    public BookingPayload build() {
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
