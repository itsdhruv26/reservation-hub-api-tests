package com.reservationhub.applicationapi;

import common.core.api.RestResourceService;
import common.template.HttpReqObject;
import io.restassured.response.Response;

import static common.core.api.Routes.AUTH;

/** One method per Auth API endpoint. */
public class AuthFlowRequests {

    public static Response createToken(HttpReqObject httpReqObject) {
        return RestResourceService.post(AUTH, httpReqObject);
    }
}
