package com.reservationhub.http;

import io.restassured.specification.RequestSpecification;

/**
 * How a write request authenticates. Modelled as a value so auth-rejection tests can be
 * parameterised over credential types instead of duplicating request code.
 */
@FunctionalInterface
public interface Auth {

    RequestSpecification applyTo(RequestSpecification request);

    static Auth none() {
        return request -> request;
    }

    /** The API's primary scheme: the token from POST /auth sent as a cookie. */
    static Auth token(String token) {
        return request -> request.cookie("token", token);
    }

    static Auth basic(String username, String password) {
        return request -> request.auth().preemptive().basic(username, password);
    }
}
