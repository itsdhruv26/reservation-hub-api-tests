package com.reservationhub.core.tokenmanager;

import common.core.api.Auth;

/** The two credential schemes the API documents for write calls. */
public enum AuthScheme {

    TOKEN_COOKIE,
    BASIC_AUTH;

    /** Credentials of the configured user in this scheme. */
    public Auth toAuth() {
        return this == TOKEN_COOKIE ? BookingTokenManager.getTokenAuth() : BookingTokenManager.getBasicAuth();
    }
}
