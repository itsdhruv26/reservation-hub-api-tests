package com.reservationhub.testmodules.auth;

import com.reservationhub.requestbuilder.RequestBuilderAuth;
import com.reservationhub.requestbuilder.RequestBuilderBooking;
import com.reservationhub.core.BaseTest;
import com.reservationhub.core.tokenmanager.BookingTokenManager;
import com.reservationhub.dataprovider.BookingWriteCall;
import com.reservationhub.dataprovider.ReservationHubDataProvider;
import com.reservationhub.dataprovider.ReservationHubDataProvider.AuthScheme;
import com.reservationhub.pojo.auth.request.AuthReqPayload;
import com.reservationhub.pojo.createbooking.response.CreateBookingRespPayload;
import com.reservationhub.pojo.patchbooking.request.PatchBookingReqPayload;
import com.reservationhub.utilities.Named;
import common.core.api.Auth;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import static com.reservationhub.utilities.ReservationHubConstants.AUTH_TOKEN_SCHEMA;
import static com.reservationhub.utilities.ReservationHubConstants.KNOWN_DEFECT;
import static com.reservationhub.utilities.ReservationHubConstants.SMOKE;
import static common.core.utils.Assertions.assertMatchesSchema;
import static common.core.utils.Assertions.assertStatusCode;
import static common.core.utils.Assertions.assertStatusCodeIn;
import static org.assertj.core.api.Assertions.assertThat;

@Epic("Bookings API")
@Feature("Authentication")
public class AuthTest extends BaseTest {

    @Test(description = "Valid credentials return a token", groups = SMOKE)
    @Story("Token retrieval")
    @Severity(SeverityLevel.BLOCKER)
    public void validCredentialsReturnToken() {
        Response response = authController.requestToken(
                RequestBuilderAuth.genAuthPayload(BookingTokenManager.getAuthUserDetails()));

        assertStatusCode(response, 200, "POST /auth with valid credentials should succeed");
        assertMatchesSchema(response, AUTH_TOKEN_SCHEMA);
    }

    @Test(description = "Invalid credentials are rejected with 401 and no token",
            dataProvider = "invalid_credentials", dataProviderClass = ReservationHubDataProvider.class,
            groups = KNOWN_DEFECT)
    @Issue("BUG-09")
    @Story("Token retrieval")
    @Severity(SeverityLevel.NORMAL)
    public void invalidCredentialsAreRejected(String scenario, AuthReqPayload credentials) {
        Response response = authController.requestToken(credentials);

        assertThat(response.asString()).as("No token may be issued for invalid credentials").doesNotContain("\"token\"");
        assertStatusCode(response, 401, "A failed login should be signalled with HTTP 401, not a 200 the client has to parse");
    }

    @Test(description = "Unauthenticated writes are rejected and change nothing",
            dataProvider = "unauthenticated_writes", dataProviderClass = ReservationHubDataProvider.class)
    @Story("Write operations require auth")
    @Severity(SeverityLevel.CRITICAL)
    public void unauthenticatedWritesAreRejected(Named<BookingWriteCall> write, Named<Auth> badAuth) {
        CreateBookingRespPayload existing = bookingController.givenExistingBooking();

        Response response = write.value().send(bookingController, existing.getBookingid(), badAuth.value());

        // 401 is the textbook answer and the API sends 403. Both deny access, which is the risk that matters here.
        assertStatusCodeIn(response, "A write without valid credentials must be denied", 401, 403);
        assertThat(bookingController.fetchBooking(existing.getBookingid()))
                .as("A rejected write must not modify the booking")
                .isEqualTo(existing.getBooking());
    }

    @Test(description = "Both documented auth schemes are accepted for writes",
            dataProvider = "auth_schemes", dataProviderClass = ReservationHubDataProvider.class)
    @Story("Write operations require auth")
    @Severity(SeverityLevel.CRITICAL)
    public void supportedAuthSchemesAreAccepted(AuthScheme scheme) {
        Auth auth = scheme == AuthScheme.TOKEN_COOKIE ? BookingTokenManager.getTokenAuth() : BookingTokenManager.getBasicAuth();
        CreateBookingRespPayload existing = bookingController.givenExistingBooking();

        Response response = bookingController.patchBooking(existing.getBookingid(),
                PatchBookingReqPayload.builder().firstname("Renamed").build(), auth);

        assertStatusCode(response, 200, "An authenticated PATCH should succeed");
        assertThat(bookingController.fetchBooking(existing.getBookingid()))
                .isEqualTo(RequestBuilderBooking.from(existing.getBooking()).firstname("Renamed").build());
    }
}
