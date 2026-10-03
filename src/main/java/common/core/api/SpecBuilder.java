package common.core.api;

import common.core.utils.ConfigLoader;
import common.template.HttpReqObject;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

/** Shared request configuration: base URL, JSON headers, timeouts, retry, reporting and console logging. */
public final class SpecBuilder {

    /**
     * Exact value on purpose: ContentType.JSON expands to a list of four media types, which this API answers
     * with 418 on POST /booking.
     */
    public static final String DEFAULT_ACCEPT = "application/json";

    private SpecBuilder() {
    }

    public static RequestSpecification getRequestSpec(HttpReqObject httpReqObject) {
        ConfigLoader config = ConfigLoader.getInstance();
        RequestSpecBuilder builder = new RequestSpecBuilder()
                .setBaseUri(config.getBaseUrl())
                .setContentType(ContentType.JSON)
                .setAccept(DEFAULT_ACCEPT)
                .setConfig(RestAssuredConfig.config().httpClient(HttpClientConfig.httpClientConfig()
                        .setParam("http.connection.timeout", config.getConnectTimeoutMs())
                        .setParam("http.socket.timeout", config.getReadTimeoutMs())));

        if (httpReqObject.isReported()) {
            // Reporting filters go first so they record the final outcome after any retries.
            builder.addFilter(new AllureRestAssured())
                    .addFilter(new ExtentLoggingFilter());
        }
        builder.addFilter(new TransientFailureRetryFilter(config.getRetryMaxAttempts(), config.getRetryBackoffMs()));
        if (httpReqObject.isReported() && config.isVerboseConsole()) {
            // After the retry filter, so every attempt is printed, not just the last one.
            builder.addFilter(new RequestLoggingFilter())
                    .addFilter(new ResponseLoggingFilter());
        }
        return builder.build();
    }
}
