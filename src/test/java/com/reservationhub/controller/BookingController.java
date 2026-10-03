package com.reservationhub.controller;

import com.reservationhub.http.Auth;
import com.reservationhub.http.RequestSpecs;
import com.reservationhub.model.Booking;
import com.reservationhub.model.BookingPatch;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.Map;

/**
 * Wraps the /booking endpoints. Methods return the raw {@link Response} so tests can assert on status,
 * headers and body themselves; no assertions live here.
 *
 * Normal calls take the typed POJOs. The {@code *Raw} variants take an Object body for negative tests
 * that send what a POJO can't represent (wrong types, missing fields, malformed JSON).
 */
public final class BookingController extends BaseController {

    public BookingController() {
        this(RequestSpecs.reported());
    }

    public BookingController(RequestSpecification spec) {
        super(spec);
    }

    public Response create(Booking booking) {
        return createRaw(booking);
    }

    public Response createRaw(Object body) {
        return request().body(body).post("/booking");
    }

    /** Content-negotiation probe: replaces the spec's Accept header with the given value. */
    public Response create(Booking booking, String acceptHeader) {
        return request().accept(acceptHeader).body(booking).post("/booking");
    }

    public Response get(int id) {
        return getRaw(id);
    }

    /** id is Object so tests can also probe non-numeric path values. */
    public Response getRaw(Object id) {
        return request().get("/booking/{id}", id);
    }

    public Response list(Map<String, ?> filters) {
        return request().queryParams(filters).get("/booking");
    }

    public Response update(int id, Booking booking, Auth auth) {
        return updateRaw(id, booking, auth);
    }

    public Response updateRaw(int id, Object body, Auth auth) {
        return request(auth).body(body).put("/booking/{id}", id);
    }

    public Response patch(int id, BookingPatch patch, Auth auth) {
        return request(auth).body(patch).patch("/booking/{id}", id);
    }

    public Response delete(int id, Auth auth) {
        return request(auth).delete("/booking/{id}", id);
    }

    public Response ping() {
        return request().get("/ping");
    }
}
