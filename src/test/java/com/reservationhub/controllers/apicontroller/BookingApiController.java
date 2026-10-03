package com.reservationhub.controllers.apicontroller;

import com.reservationhub.requestbuilder.RequestBuilderBooking;
import com.reservationhub.applicationapi.BookingFlowRequests;
import com.reservationhub.core.tokenmanager.BookingTokenManager;
import com.reservationhub.core.utils.BookingCleanup;
import com.reservationhub.pojo.booking.BookingPayload;
import com.reservationhub.pojo.createbooking.response.CreateBookingRespPayload;
import com.reservationhub.pojo.getbookingids.response.BookingIdRespPayload;
import com.reservationhub.pojo.patchbooking.request.PatchBookingReqPayload;
import common.core.api.Auth;
import common.exception.FrameworkException;
import common.template.HttpReqObject;
import io.qameta.allure.Allure;
import io.restassured.response.Response;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static common.core.utils.Assertions.assertStatusCode;

/**
 * Steps for the /booking endpoints, in two kinds:
 * <ul>
 *   <li>Endpoint calls (create, get, update, ...) return the raw {@link Response}, so a test asserts on
 *       status, headers and body itself. Their body parameter is Object, so negative tests can also send
 *       what a POJO can't represent (wrong types, missing fields, malformed JSON).</li>
 *   <li>Given/fetch steps set up or read back state and fail fast when they can't.</li>
 * </ul>
 * Every booking the API creates through this controller is tracked and deleted after the test, including
 * bookings it wrongly accepted.
 */
public class BookingApiController {

    // ─── Endpoint calls ───

    public Response createBooking(Object body) {
        Response response = BookingFlowRequests.createBooking(HttpReqObject.builder().requestBody(body).build());
        BookingCleanup.trackIfCreated(response);
        return response;
    }

    /** Content-negotiation probe: replaces the default Accept header with the given value. */
    public Response createBooking(BookingPayload booking, String acceptHeader) {
        Response response = BookingFlowRequests.createBooking(HttpReqObject.builder()
                .requestBody(booking).accept(acceptHeader).build());
        BookingCleanup.trackIfCreated(response);
        return response;
    }

    /**
     * Unreported and untracked, for calls made from worker threads: Allure's per-request attachments are not
     * thread-safe, and cleanup tracking is per thread, so the caller tracks what was created.
     */
    public Response createBookingUnreported(BookingPayload booking) {
        return BookingFlowRequests.createBooking(HttpReqObject.builder().requestBody(booking).reported(false).build());
    }

    /** id is Object so tests can also probe non-numeric path values. */
    public Response getBooking(Object id) {
        return BookingFlowRequests.getBooking(HttpReqObject.builder().pathParams(Map.of("id", id)).build());
    }

    public Response searchBookings(Map<String, ?> filters) {
        return BookingFlowRequests.getBookingIds(HttpReqObject.builder().queryParams(filters).build());
    }

    public Response updateBooking(int id, Object body, Auth auth) {
        return BookingFlowRequests.updateBooking(HttpReqObject.builder()
                .pathParams(Map.of("id", id)).requestBody(body).auth(auth).build());
    }

    public Response patchBooking(int id, PatchBookingReqPayload patch, Auth auth) {
        return BookingFlowRequests.partialUpdateBooking(HttpReqObject.builder()
                .pathParams(Map.of("id", id)).requestBody(patch).auth(auth).build());
    }

    public Response deleteBooking(int id, Auth auth) {
        return BookingFlowRequests.deleteBooking(HttpReqObject.builder()
                .pathParams(Map.of("id", id)).auth(auth).build());
    }

    public Response ping() {
        return BookingFlowRequests.healthCheck(HttpReqObject.builder().build());
    }

    // ─── Given / fetch steps ───

    public CreateBookingRespPayload givenExistingBooking() {
        return givenExistingBooking(RequestBuilderBooking.aBooking().build());
    }

    /**
     * Setup failures throw FrameworkException rather than an assertion error, so the reports mark the test
     * "broken" (couldn't reach a verdict) instead of "failed" (product defect).
     */
    public CreateBookingRespPayload givenExistingBooking(BookingPayload booking) {
        return Allure.step("Given an existing booking", () -> {
            Response response = createBooking(booking);
            if (response.statusCode() != 200) {
                throw new FrameworkException("Precondition failed: could not create booking. HTTP "
                        + response.statusCode() + ", body: " + response.asString());
            }
            return response.as(CreateBookingRespPayload.class);
        });
    }

    /** An id that is known not to exist: a booking created and then deleted by this test. */
    public int givenDeletedBookingId() {
        int id = givenExistingBooking().getBookingid();
        Response deletion = deleteBooking(id, BookingTokenManager.getBasicAuth());
        if (deletion.statusCode() >= 300) {
            throw new FrameworkException("Precondition failed: could not delete booking " + id
                    + ". HTTP " + deletion.statusCode());
        }
        return id;
    }

    /** Reads a booking back from the API: the source of truth for "was the change actually persisted?". */
    public BookingPayload fetchBooking(int id) {
        return Allure.step("Read booking " + id + " back from the API", () -> {
            Response response = getBooking(id);
            assertStatusCode(response, 200, "Booking " + id + " should be retrievable");
            return response.as(BookingPayload.class);
        });
    }

    /** Ids returned by GET /booking for the given filters, e.g. {@code Map.of("firstname", "Jim")}. */
    public List<Integer> searchBookingIds(Map<String, ?> filters) {
        Response response = searchBookings(filters);
        assertStatusCode(response, 200, "Search with filters " + filters + " should succeed");
        return Arrays.stream(response.as(BookingIdRespPayload[].class)).map(BookingIdRespPayload::getBookingid).toList();
    }
}
