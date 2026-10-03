package com.reservationhub.controller;

import com.reservationhub.http.RequestSpecs;
import com.reservationhub.config.Config;
import com.reservationhub.model.AuthRequest;
import com.reservationhub.model.AuthToken;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

/** Wraps POST /auth. */
public final class AuthController extends BaseController {

    public AuthController() {
        this(RequestSpecs.reported());
    }

    public AuthController(RequestSpecification spec) {
        super(spec);
    }

    public Response requestToken(AuthRequest credentials) {
        return request().body(credentials).post("/auth");
    }

    /**
     * Fetches a fresh token with the configured credentials. Tokens are not cached across tests:
     * the sandbox periodically resets and a cached token could silently go stale mid-run.
     */
    public String validToken() {
        Response response = requestToken(AuthRequest.of(Config.username(), Config.password()));
        String token = response.statusCode() == 200 ? response.as(AuthToken.class).getToken() : null;
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Precondition failed: could not obtain an auth token. HTTP "
                    + response.statusCode() + ", body: " + response.asString());
        }
        return token;
    }
}
