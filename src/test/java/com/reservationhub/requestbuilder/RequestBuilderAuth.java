package com.reservationhub.requestbuilder;

import com.reservationhub.pojo.auth.request.AuthReqPayload;

import java.util.Map;

public class RequestBuilderAuth {

    /** Credentials of a user from USERS in yml/{env}.yml. */
    public static AuthReqPayload genAuthPayload(Map<String, Object> userDetails) {
        return genAuthPayload(userDetails.get("username").toString(), userDetails.get("password").toString());
    }

    public static AuthReqPayload genAuthPayload(String username, String password) {
        return AuthReqPayload.builder()
                .username(username)
                .password(password)
                .build();
    }

    /** Null fields are omitted, so this is sent as an empty JSON object. */
    public static AuthReqPayload genEmptyAuthPayload() {
        return new AuthReqPayload();
    }
}
