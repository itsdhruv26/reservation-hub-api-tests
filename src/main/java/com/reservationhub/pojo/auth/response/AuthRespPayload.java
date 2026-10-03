package com.reservationhub.pojo.auth.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response body of POST /auth: {@code token} on success, {@code reason} on a (200 OK) failed login. */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthRespPayload {

    private String token;
    private String reason;
}
