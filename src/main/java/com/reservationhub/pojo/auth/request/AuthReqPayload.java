package com.reservationhub.pojo.auth.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request body of POST /auth. A null field is omitted, so {@code new AuthReqPayload()} is an empty body. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthReqPayload {

    private String username;
    private String password;

    public static AuthReqPayload of(String username, String password) {
        return new AuthReqPayload(username, password);
    }
}
