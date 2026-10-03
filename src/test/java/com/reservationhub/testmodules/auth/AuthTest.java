package com.reservationhub.testmodules.auth;

import com.reservationhub.core.BaseTest;
import com.reservationhub.core.tokenmanager.AuthScheme;
import com.reservationhub.dataprovider.BookingWriteCall;
import com.reservationhub.dataprovider.ReservationHubDataProvider;
import com.reservationhub.pojo.auth.request.AuthReqPayload;
import com.reservationhub.utilities.Named;
import common.core.api.Auth;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.testng.annotations.Test;

import static com.reservationhub.utilities.ReservationHubConstants.KNOWN_DEFECT;
import static com.reservationhub.utilities.ReservationHubConstants.SMOKE;

@Epic("Bookings API")
@Feature("Authentication")
public class AuthTest extends BaseTest {

    @Test(description = "Valid credentials return a token", groups = SMOKE)
    @Story("Token retrieval")
    @Severity(SeverityLevel.BLOCKER)
    public void validCredentialsReturnToken() {
        authController.verifyValidCredentialsReturnToken();
    }

    @Test(description = "Invalid credentials are rejected with 401 and no token",
            dataProvider = "invalid_credentials", dataProviderClass = ReservationHubDataProvider.class,
            groups = KNOWN_DEFECT)
    @Issue("BUG-09")
    @Story("Token retrieval")
    @Severity(SeverityLevel.NORMAL)
    public void invalidCredentialsAreRejected(String scenario, AuthReqPayload credentials) {
        authController.verifyInvalidCredentialsRejected(credentials);
    }

    @Test(description = "Unauthenticated writes are rejected and change nothing",
            dataProvider = "unauthenticated_writes", dataProviderClass = ReservationHubDataProvider.class)
    @Story("Write operations require auth")
    @Severity(SeverityLevel.CRITICAL)
    public void unauthenticatedWritesAreRejected(Named<BookingWriteCall> write, Named<Auth> badAuth) {
        bookingController.verifyWriteWithoutValidCredentialsDenied(write.value(), badAuth.value());
    }

    @Test(description = "Both documented auth schemes are accepted for writes",
            dataProvider = "auth_schemes", dataProviderClass = ReservationHubDataProvider.class)
    @Story("Write operations require auth")
    @Severity(SeverityLevel.CRITICAL)
    public void supportedAuthSchemesAreAccepted(AuthScheme scheme) {
        bookingController.verifyAuthSchemeAcceptedForWrites(scheme);
    }
}
