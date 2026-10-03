package com.reservationhub.controllers.apicontroller;

import com.reservationhub.requestbuilder.RequestBuilderAuth;
import com.reservationhub.applicationapi.AuthFlowRequests;
import com.reservationhub.pojo.auth.request.AuthReqPayload;
import com.reservationhub.pojo.auth.response.AuthRespPayload;
import common.exception.FrameworkException;
import common.template.HttpReqObject;
import io.restassured.response.Response;

import java.util.Map;

/** Steps for POST /auth. */
public class AuthApiController {

    /** Raw call: the test decides what the response means. */
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
}
