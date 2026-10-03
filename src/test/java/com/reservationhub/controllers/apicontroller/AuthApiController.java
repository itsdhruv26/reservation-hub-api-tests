package com.reservationhub.controllers.apicontroller;

import com.reservationhub.requestbuilder.RequestBuilderAuth;
import com.reservationhub.applicationapi.AuthFlowRequests;
import com.reservationhub.core.tokenmanager.BookingTokenManager;
import com.reservationhub.pojo.auth.request.AuthReqPayload;
import com.reservationhub.pojo.auth.response.AuthRespPayload;
import common.exception.FrameworkException;
import common.template.HttpReqObject;
import io.restassured.response.Response;

import java.util.Map;

import static com.reservationhub.utilities.ReservationHubConstants.AUTH_TOKEN_SCHEMA;
import static common.core.utils.Assertions.assertMatchesSchema;
import static common.core.utils.Assertions.assertStatusCode;
import static org.assertj.core.api.Assertions.assertThat;

/** Calls and scenarios for POST /auth. Credentials on write calls are covered by {@link BookingApiController}. */
public class AuthApiController {

    public Response requestToken(AuthReqPayload credentials) {
        return AuthFlowRequests.createToken(HttpReqObject.builder().requestBody(credentials).build());
    }

    /**
     * Fetches a token for the given user from yml/{env}.yml. A failure here is a broken precondition, not a
     * finding, so it throws FrameworkException rather than an assertion error.
     */
    public String generateToken(Map<String, Object> userDetails) {
        Response response = requestToken(RequestBuilderAuth.genAuthPayload(userDetails));
        String token = response.statusCode() == 200 ? response.as(AuthRespPayload.class).getToken() : null;
        if (token == null || token.isBlank()) {
            throw new FrameworkException("Precondition failed: could not obtain an auth token. HTTP "
                    + response.statusCode() + ", body: " + response.asString());
        }
        return token;
    }

    // ─── Scenarios ───

    /** The user configured by {@code authUser} gets a token in the documented shape. */
    public void verifyValidCredentialsReturnToken() {
        Response response = requestToken(RequestBuilderAuth.genAuthPayload(BookingTokenManager.getAuthUserDetails()));

        assertStatusCode(response, 200, "POST /auth with valid credentials should succeed");
        assertMatchesSchema(response, AUTH_TOKEN_SCHEMA);
    }

    /** No token in the body, and the failure signalled with HTTP 401. */
    public void verifyInvalidCredentialsRejected(AuthReqPayload credentials) {
        Response response = requestToken(credentials);

        assertThat(response.asString()).as("No token may be issued for invalid credentials").doesNotContain("\"token\"");
        assertStatusCode(response, 401, "A failed login should be signalled with HTTP 401, not a 200 the client has to parse");
    }
}
