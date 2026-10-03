package com.reservationhub.http;

import com.reservationhub.config.Config;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

/** Shared request configuration: base URL, JSON headers, timeouts, retry and reporting. */
public final class RequestSpecs {

    private RequestSpecs() {
    }

    /** Spec for test traffic: every request/response is attached to the Allure report. */
    public static RequestSpecification reported() {
        return builder()
                // Allure goes first so it records the final outcome after any retries.
                .addFilter(new AllureRestAssured())
                .addFilter(retryFilter())
                .build();
    }

    /** Spec for housekeeping traffic (cleanup, warm-up, concurrent calls) that would only add noise to the report. */
    public static RequestSpecification unreported() {
        return builder()
                .addFilter(retryFilter())
                .build();
    }

    private static RequestSpecBuilder builder() {
        return new RequestSpecBuilder()
                .setBaseUri(Config.baseUrl())
                .setContentType(ContentType.JSON)
                // Exact value on purpose: ContentType.JSON expands to a list of four media types,
                // which this API answers with 418 on POST /booking.
                .setAccept("application/json")
                .setConfig(RestAssuredConfig.config().httpClient(HttpClientConfig.httpClientConfig()
                        .setParam("http.connection.timeout", Config.connectTimeoutMs())
                        .setParam("http.socket.timeout", Config.readTimeoutMs())));
    }

    private static TransientFailureRetryFilter retryFilter() {
        return new TransientFailureRetryFilter(Config.retryMaxAttempts(), Config.retryBackoffMs());
    }
}
