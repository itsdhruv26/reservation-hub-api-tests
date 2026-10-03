package com.reservationhub.support;

import io.restassured.response.Response;

import java.util.Arrays;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Assertions whose failure messages read as a finding: what we expected, in business terms,
 * and what the API actually returned. Full request/response is attached to the report separately.
 */
public final class ApiAssertions {

    private static final int MAX_BODY_IN_MESSAGE = 500;

    private ApiAssertions() {
    }

    public static void assertStatus(Response response, int expected, String expectation) {
        assertThat(response.statusCode())
                .as("%s%nResponse body: %s", expectation, abbreviatedBody(response))
                .isEqualTo(expected);
    }

    public static void assertStatusIn(Response response, String expectation, Integer... allowed) {
        assertThat(response.statusCode())
                .as("%s (allowed: %s)%nResponse body: %s", expectation, Arrays.toString(allowed), abbreviatedBody(response))
                .isIn((Object[]) allowed);
    }

    /** Schema files live in src/test/resources/schemas. */
    public static void assertMatchesSchema(Response response, String schemaFile) {
        response.then().assertThat().body(matchesJsonSchemaInClasspath("schemas/" + schemaFile));
    }

    private static String abbreviatedBody(Response response) {
        String body = response.asString();
        return body.length() <= MAX_BODY_IN_MESSAGE ? body : body.substring(0, MAX_BODY_IN_MESSAGE) + "...";
    }
}
