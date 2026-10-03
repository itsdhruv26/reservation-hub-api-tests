package com.reservationhub.applicationapi;

import common.core.api.RestResourceService;
import common.template.HttpReqObject;
import io.restassured.response.Response;

import static common.core.api.Routes.BOOKING;
import static common.core.api.Routes.BOOKING_BY_ID;
import static common.core.api.Routes.PING;

/** One method per Booking API endpoint. No payload building and no assertions here: see the controllers. */
public class BookingFlowRequests {

    public static Response createBooking(HttpReqObject httpReqObject) {
        return RestResourceService.post(BOOKING, httpReqObject);
    }

    public static Response getBookingIds(HttpReqObject httpReqObject) {
        return RestResourceService.get(BOOKING, httpReqObject);
    }

    public static Response getBooking(HttpReqObject httpReqObject) {
        return RestResourceService.get(BOOKING_BY_ID, httpReqObject);
    }

    public static Response updateBooking(HttpReqObject httpReqObject) {
        return RestResourceService.put(BOOKING_BY_ID, httpReqObject);
    }

    public static Response partialUpdateBooking(HttpReqObject httpReqObject) {
        return RestResourceService.patch(BOOKING_BY_ID, httpReqObject);
    }

    public static Response deleteBooking(HttpReqObject httpReqObject) {
        return RestResourceService.delete(BOOKING_BY_ID, httpReqObject);
    }

    public static Response healthCheck(HttpReqObject httpReqObject) {
        return RestResourceService.get(PING, httpReqObject);
    }
}
