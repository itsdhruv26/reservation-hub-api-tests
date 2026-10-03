package com.reservationhub.dataprovider;

import com.reservationhub.controllers.apicontroller.BookingApiController;
import com.reservationhub.core.tokenmanager.BookingTokenManager;
import common.core.api.Auth;
import io.restassured.response.Response;

/** A write request (PUT, PATCH or DELETE) against an existing booking, parameterised by the credential to send. */
@FunctionalInterface
public interface BookingWriteCall {

    Response send(BookingApiController controller, int bookingId, Auth auth);

    /** Sent with valid token credentials, as a partner would. */
    default Response send(BookingApiController controller, int bookingId) {
        return send(controller, bookingId, BookingTokenManager.getTokenAuth());
    }
}
