package com.reservationhub.controller;

import com.reservationhub.http.Auth;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;

/** Holds the request spec shared by every endpoint group. Controllers make calls and never assert. */
abstract class BaseController {

    private final RequestSpecification spec;

    BaseController(RequestSpecification spec) {
        this.spec = spec;
    }

    RequestSpecification request() {
        return given().spec(spec);
    }

    RequestSpecification request(Auth auth) {
        return auth.applyTo(request());
    }
}
