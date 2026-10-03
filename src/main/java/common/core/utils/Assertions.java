package common.core.utils;

import common.extent.Logger;
import io.restassured.response.Response;

import java.util.Arrays;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Response assertions whose failure messages read as a finding: what we expected, in business terms, and what
 * the API actually returned. Each check is also logged to the Extent report; the full request/response is
 * attached to the reports separately.
 */
public final class Assertions {

    private static final int MAX_BODY_IN_MESSAGE = 500;

    private Assertions() {
    }

    public static void assertStatusCode(Response response, int expected, String expectation) {
        log(response.statusCode() == expected, expectation + " → expected " + expected + ", got " + response.statusCode());
        assertThat(response.statusCode())
                .as("%s%nResponse body: %s", expectation, abbreviatedBody(response))
                .isEqualTo(expected);
    }

    public static void assertStatusCodeIn(Response response, String expectation, Integer... allowed) {
        boolean ok = Arrays.asList(allowed).contains(response.statusCode());
        log(ok, expectation + " → expected one of " + Arrays.toString(allowed) + ", got " + response.statusCode());
        assertThat(response.statusCode())
                .as("%s (allowed: %s)%nResponse body: %s", expectation, Arrays.toString(allowed), abbreviatedBody(response))
                .isIn((Object[]) allowed);
    }

    /** Schema files live in src/main/resources/schemas. */
    public static void assertMatchesSchema(Response response, String schemaFile) {
        response.then().assertThat().body(matchesJsonSchemaInClasspath("schemas/" + schemaFile));
        Logger.logSuccess("Response matches schema " + schemaFile);
    }

    private static void log(boolean passed, String message) {
        if (passed) {
            Logger.logSuccess(message);
        } else {
            Logger.logFailure(message);
        }
    }

    private static String abbreviatedBody(Response response) {
        String body = response.asString();
        return body.length() <= MAX_BODY_IN_MESSAGE ? body : body.substring(0, MAX_BODY_IN_MESSAGE) + "...";
    }
}
