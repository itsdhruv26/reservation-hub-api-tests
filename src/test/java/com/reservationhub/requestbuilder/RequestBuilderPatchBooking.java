package com.reservationhub.requestbuilder;

import com.reservationhub.pojo.booking.BookingDates;
import com.reservationhub.pojo.booking.BookingPayload;
import com.reservationhub.pojo.patchbooking.request.PatchBookingReqPayload;

import java.time.LocalDate;

/**
 * Builds PATCH /booking/{id} bodies. Only the fields a patch sets are sent; pair a patch with
 * {@link RequestBuilderBooking#genExpectedAfterPatch} to get the state the booking should end up in.
 */
public final class RequestBuilderPatchBooking {

    private RequestBuilderPatchBooking() {
    }

    public static PatchBookingReqPayload genFirstnamePatch(String firstname) {
        return PatchBookingReqPayload.builder().firstname(firstname).build();
    }

    public static PatchBookingReqPayload genAdditionalNeedsPatch(String additionalneeds) {
        return PatchBookingReqPayload.builder().additionalneeds(additionalneeds).build();
    }

    public static PatchBookingReqPayload genPricePatch(int totalprice) {
        return PatchBookingReqPayload.builder().totalprice(totalprice).build();
    }

    public static PatchBookingReqPayload genStayPatch(LocalDate checkin, LocalDate checkout) {
        return PatchBookingReqPayload.builder().bookingdates(BookingDates.of(checkin, checkout)).build();
    }

    /** Two top-level fields at once: a new first name and a total 25 higher than {@code original}'s. */
    public static PatchBookingReqPayload genRenameAndRepricePatch(BookingPayload original) {
        return PatchBookingReqPayload.builder()
                .firstname("Amended")
                .totalprice(original.getTotalprice() + 25)
                .build();
    }

    /** Only the nested check-out, moved {@code extraNights} later than {@code original}'s. */
    public static PatchBookingReqPayload genExtendStayPatch(BookingPayload original, int extraNights) {
        String extendedCheckout = LocalDate.parse(original.getBookingdates().getCheckout()).plusDays(extraNights).toString();
        return PatchBookingReqPayload.builder()
                .bookingdates(BookingDates.builder().checkout(extendedCheckout).build())
                .build();
    }
}
