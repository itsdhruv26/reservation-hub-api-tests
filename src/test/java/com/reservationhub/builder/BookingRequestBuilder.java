package com.reservationhub.builder;

import com.reservationhub.model.Booking;
import com.reservationhub.model.BookingDates;

import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Fluent builder for booking request bodies. It starts from a valid booking that is unique per call, so
 * a test can always find "its" booking (e.g. via name filters) on a shared instance full of other
 * people's data, and then breaks or tweaks only the fields a test cares about:
 *
 * <pre>{@code
 * Booking incident = BookingRequestBuilder.aBooking().totalprice(-100).stayFrom(checkin, checkin.minusDays(5)).build();
 * }</pre>
 *
 * {@link #from(Booking)} starts from an existing booking instead, which is how tests express the expected
 * state after an amendment.
 */
public final class BookingRequestBuilder {

    private final Booking.BookingBuilder booking;

    private BookingRequestBuilder(Booking.BookingBuilder booking) {
        this.booking = booking;
    }

    /** A valid booking with random names, a future three-night stay and a price between 50 and 999. */
    public static BookingRequestBuilder aBooking() {
        LocalDate checkin = LocalDate.now().plusDays(ThreadLocalRandom.current().nextInt(30, 365));
        return new BookingRequestBuilder(Booking.builder()
                .firstname("Qa" + uniqueSuffix())
                .lastname("Auto" + uniqueSuffix())
                .totalprice(ThreadLocalRandom.current().nextInt(50, 1000))
                .depositpaid(true)
                .bookingdates(BookingDates.of(checkin, checkin.plusDays(3)))
                .additionalneeds("Breakfast"));
    }

    public static BookingRequestBuilder from(Booking original) {
        return new BookingRequestBuilder(original.toBuilder());
    }

    /** A valid booking that differs from {@code original} in every field, for full-replacement (PUT) checks. */
    public static Booking replacementFor(Booking original) {
        LocalDate newCheckin = LocalDate.parse(original.getBookingdates().getCheckin()).plusDays(10);
        return aBooking()
                .totalprice(original.getTotalprice() + 100)
                .depositpaid(!original.getDepositpaid())
                .stayFrom(newCheckin, newCheckin.plusDays(5))
                .additionalneeds("Late checkout")
                .build();
    }

    public BookingRequestBuilder firstname(String value) {
        booking.firstname(value);
        return this;
    }

    public BookingRequestBuilder lastname(String value) {
        booking.lastname(value);
        return this;
    }

    public BookingRequestBuilder totalprice(Integer value) {
        booking.totalprice(value);
        return this;
    }

    public BookingRequestBuilder depositpaid(Boolean value) {
        booking.depositpaid(value);
        return this;
    }

    public BookingRequestBuilder bookingdates(BookingDates value) {
        booking.bookingdates(value);
        return this;
    }

    public BookingRequestBuilder stayFrom(LocalDate checkin, LocalDate checkout) {
        return bookingdates(BookingDates.of(checkin, checkout));
    }

    /** Pass null to leave the optional field out of the request. */
    public BookingRequestBuilder additionalneeds(String value) {
        booking.additionalneeds(value);
        return this;
    }

    public Booking build() {
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
}
