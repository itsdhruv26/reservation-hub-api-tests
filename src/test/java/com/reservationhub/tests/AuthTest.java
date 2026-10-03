package com.reservationhub.tests;

import com.reservationhub.http.Auth;
import com.reservationhub.builder.BookingRequestBuilder;
import com.reservationhub.config.Config;
import com.reservationhub.controller.BookingController;
import com.reservationhub.model.AuthRequest;
import com.reservationhub.model.BookingPatch;
import com.reservationhub.model.CreatedBooking;
import com.reservationhub.support.BaseApiTest;
import com.reservationhub.support.Named;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static com.reservationhub.support.ApiAssertions.assertMatchesSchema;
import static com.reservationhub.support.ApiAssertions.assertStatus;
import static com.reservationhub.support.ApiAssertions.assertStatusIn;
import static com.reservationhub.support.Groups.KNOWN_DEFECT;
import static com.reservationhub.support.Groups.SMOKE;
import static com.reservationhub.support.Named.named;
import static org.assertj.core.api.Assertions.assertThat;

@Epic("Bookings API")
@Feature("Authentication")
public class AuthTest extends BaseApiTest {

    @Test(description = "Valid credentials return a token", groups = SMOKE)
    @Story("Token retrieval")
    @Severity(SeverityLevel.BLOCKER)
    public void validCredentialsReturnToken() {
        Response response = authController.requestToken(AuthRequest.of(Config.username(), Config.password()));

        assertStatus(response, 200, "POST /auth with valid credentials should succeed");
        assertMatchesSchema(response, "auth-token.json");
    }

    /** Each row: a readable label for the report, then the credentials to send. */
    @DataProvider
    public static Object[][] invalidCredentials() {
        return new Object[][]{
                {"wrong password", AuthRequest.of(Config.username(), "not-the-password")},
                {"unknown user", AuthRequest.of("nobody", Config.password())},
                {"empty body", new AuthRequest()}};
    }

    @Test(description = "Invalid credentials are rejected with 401 and no token",
            dataProvider = "invalidCredentials", groups = KNOWN_DEFECT)
    @Issue("BUG-09")
    @Story("Token retrieval")
    @Severity(SeverityLevel.NORMAL)
    public void invalidCredentialsAreRejected(String scenario, AuthRequest credentials) {
        Response response = authController.requestToken(credentials);

        assertThat(response.asString()).as("No token may be issued for invalid credentials").doesNotContain("\"token\"");
        assertStatus(response, 401, "A failed login should be signalled with HTTP 401, not a 200 the client has to parse");
    }

    /** A write request against an existing booking, parameterised by the credential to send. */
    @FunctionalInterface
    interface WriteCall {
        Response send(BookingController client, int bookingId, Auth auth);
    }

    /** Every write method combined with every kind of bad credential: 3 x 3 = 9 rows. */
    @DataProvider
    public static Object[][] unauthenticatedWrites() {
        WriteCall put = (client, id, auth) -> client.update(id, BookingRequestBuilder.aBooking().build(), auth);
        WriteCall patch = (client, id, auth) -> client.patch(id, BookingPatch.builder().firstname("Hacked").build(), auth);
        WriteCall delete = (client, id, auth) -> client.delete(id, auth);
        List<Named<WriteCall>> writes = List.of(named("PUT", put), named("PATCH", patch), named("DELETE", delete));
        List<Named<Auth>> badCredentials = List.of(
                named("no credentials", Auth.none()),
                named("an invalid token", Auth.token("not-a-real-token")),
                named("a wrong basic-auth password", Auth.basic(Config.username(), "wrong")));

        List<Object[]> rows = new ArrayList<>();
        for (Named<WriteCall> write : writes) {
            for (Named<Auth> auth : badCredentials) {
                rows.add(new Object[]{write, auth});
            }
        }
        return rows.toArray(new Object[0][]);
    }

    @Test(description = "Unauthenticated writes are rejected and change nothing", dataProvider = "unauthenticatedWrites")
    @Story("Write operations require auth")
    @Severity(SeverityLevel.CRITICAL)
    public void unauthenticatedWritesAreRejected(Named<WriteCall> write, Named<Auth> badAuth) {
        CreatedBooking existing = givenExistingBooking();

        Response response = write.value().send(bookings, existing.getBookingid(), badAuth.value());

        // 401 is the textbook answer and the API sends 403. Both deny access, which is the risk that matters here.
        assertStatusIn(response, "A write without valid credentials must be denied", 401, 403);
        assertThat(fetchBooking(existing.getBookingid()))
                .as("A rejected write must not modify the booking")
                .isEqualTo(existing.getBooking());
    }

    enum AuthScheme { TOKEN_COOKIE, BASIC_AUTH }

    @DataProvider
    public static Object[][] authSchemes() {
        return new Object[][]{{AuthScheme.TOKEN_COOKIE}, {AuthScheme.BASIC_AUTH}};
    }

    @Test(description = "Both documented auth schemes are accepted for writes", dataProvider = "authSchemes")
    @Story("Write operations require auth")
    @Severity(SeverityLevel.CRITICAL)
    public void supportedAuthSchemesAreAccepted(AuthScheme scheme) {
        Auth auth = scheme == AuthScheme.TOKEN_COOKIE ? validToken() : validBasicAuth();
        CreatedBooking existing = givenExistingBooking();

        Response response = bookings.patch(existing.getBookingid(), BookingPatch.builder().firstname("Renamed").build(), auth);

        assertStatus(response, 200, "An authenticated PATCH should succeed");
        assertThat(fetchBooking(existing.getBookingid())).isEqualTo(BookingRequestBuilder.from(existing.getBooking()).firstname("Renamed").build());
    }
}
