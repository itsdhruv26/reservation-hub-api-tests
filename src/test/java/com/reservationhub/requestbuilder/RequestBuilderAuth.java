package com.reservationhub.requestbuilder;

import com.reservationhub.pojo.auth.request.AuthReqPayload;

import java.util.Map;

public class RequestBuilderAuth {

    /** Credentials of a user from USERS in yml/{env}.yml. */
    public static AuthReqPayload genAuthPayload(Map<String, Object> userDetails) {
        return AuthReqPayload.builder()
                .username(userDetails.get("username").toString())
                .password(userDetails.get("password").toString())
                .build();
    }
}
