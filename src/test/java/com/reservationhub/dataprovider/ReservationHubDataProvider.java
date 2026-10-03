package com.reservationhub.dataprovider;

import com.reservationhub.requestbuilder.RequestBuilderBooking;
import com.reservationhub.core.tokenmanager.BookingTokenManager;
import com.reservationhub.pojo.auth.request.AuthReqPayload;
import com.reservationhub.pojo.booking.BookingDates;
import com.reservationhub.pojo.booking.BookingPayload;
import com.reservationhub.pojo.patchbooking.request.PatchBookingReqPayload;
import com.reservationhub.utilities.Named;
import common.core.api.Auth;
import common.core.utils.YamlReader;
import org.testng.annotations.DataProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import static com.reservationhub.requestbuilder.RequestBuilderBooking.FUTURE_CHECKIN;
import static com.reservationhub.utilities.Named.named;

/**
 * Every data provider of the suite. Rows that are not self-explanatory start with a readable label, which is
 * what TestNG and the reports print for the row.
 */
public class ReservationHubDataProvider {

    public enum AuthScheme { TOKEN_COOKIE, BASIC_AUTH }

    // ===================== AUTH =====================

    /** Each row: a readable label for the report, then the credentials to send. */
    @DataProvider(name = "invalid_credentials")
    public static Object[][] invalidCredentials() {
        Map<String, Object> user = BookingTokenManager.getAuthUserDetails();
        Map<String, Object> invalid = YamlReader.getYamlValues("INVALID_CREDENTIALS");
        String username = user.get("username").toString();
        String password = user.get("password").toString();
        return new Object[][]{
                {"wrong password", AuthReqPayload.of(username, invalid.get("wrong_password").toString())},
                {"unknown user", AuthReqPayload.of(invalid.get("unknown_username").toString(), password)},
                {"empty body", new AuthReqPayload()}};
    }

    /** Every write method combined with every kind of bad credential: 3 x 3 = 9 rows. */
    @DataProvider(name = "unauthenticated_writes")
    public static Object[][] unauthenticatedWrites() {
        Map<String, Object> user = BookingTokenManager.getAuthUserDetails();
        Map<String, Object> invalid = YamlReader.getYamlValues("INVALID_CREDENTIALS");
        List<Named<Auth>> badCredentials = List.of(
                named("no credentials", Auth.none()),
                named("an invalid token", Auth.token(invalid.get("invalid_token").toString())),
                named("a wrong basic-auth password", Auth.basic(user.get("username").toString(),
                        invalid.get("wrong_password").toString())));

        List<Object[]> rows = new ArrayList<>();
        for (Named<BookingWriteCall> write : writeCallList("Hacked")) {
            for (Named<Auth> auth : badCredentials) {
                rows.add(new Object[]{write, auth});
            }
        }
        return rows.toArray(new Object[0][]);
    }

    @DataProvider(name = "auth_schemes")
    public static Object[][] authSchemes() {
        return new Object[][]{{AuthScheme.TOKEN_COOKIE}, {AuthScheme.BASIC_AUTH}};
    }

    // ===================== CREATE / SEARCH =====================

    /** What common HTTP clients send by default (axios, browsers, REST Assured). */
    @DataProvider(name = "standard_accept_headers")
    public static Object[][] standardAcceptHeaders() {
        return new Object[][]{
                {"application/json, text/plain, */*"},
                {"application/json;q=0.9"}};
    }

    /**
     * The docs say: checkin returns bookings with check-in >= the given date, and checkout returns
     * bookings with check-out >= the given date. Each case puts the filter date just before, on or after
     * the booking's own date to probe the boundary.
     * Each row: filter, filter date relative to the booking's date (days), expected to be included.
     * <p>
     * Split in two so the cases the API gets right stay in the green gate (-DexcludedGroups=known-defect).
     * When BUG-07 is fixed, move the known-defect rows back into the first provider.
     */
    @DataProvider(name = "date_filter_boundaries")
    public static Object[][] dateFilterBoundaries() {
        return new Object[][]{
                {"checkin", -1, true},
                {"checkin", 1, false},
                {"checkout", 0, true}};
    }

    /** The cases BUG-07 breaks: the same-day check-in, and the check-out filter behaving as "on or before". */
    @DataProvider(name = "date_filter_boundaries_known_defect")
    public static Object[][] dateFilterBoundariesKnownDefect() {
        return new Object[][]{
                {"checkin", 0, true},
                {"checkout", -1, true},
                {"checkout", 1, false}};
    }

    // ===================== UPDATE (PUT) / AMEND (PATCH) =====================

    /** Each row: how to corrupt an otherwise valid booking, with a readable name for the report. */
    @DataProvider(name = "invalid_replacements")
    public static Object[][] invalidReplacements() {
        UnaryOperator<BookingPayload> negativePrice = b -> RequestBuilderBooking.from(b).totalprice(-50).build();
        UnaryOperator<BookingPayload> invertedDates = b -> RequestBuilderBooking.from(b).stayFrom(FUTURE_CHECKIN, FUTURE_CHECKIN.minusDays(2)).build();
        return new Object[][]{
                {named("negative total price", negativePrice)},
                {named("check-out before check-in", invertedDates)}};
    }

    /** Each row: a readable label for the report, then the PATCH body to send. */
    @DataProvider(name = "invalid_patches")
    public static Object[][] invalidPatches() {
        return new Object[][]{
                {"negative total price", PatchBookingReqPayload.builder().totalprice(-1).build()},
                {"check-out before check-in", PatchBookingReqPayload.builder()
                        .bookingdates(BookingDates.of(FUTURE_CHECKIN, FUTURE_CHECKIN.minusDays(3))).build()}};
    }

    // ===================== NOT FOUND =====================

    /** Each row: the write call to make, named by its HTTP method. */
    @DataProvider(name = "write_calls")
    public static Object[][] writeCalls() {
        return writeCallList("Ghost").stream().map(call -> new Object[]{call}).toArray(Object[][]::new);
    }

    // ===================== INPUT VALIDATION ON CREATE =====================

    @DataProvider(name = "negative_prices")
    public static Object[][] negativePrices() {
        return new Object[][]{{-1}, {-100}};
    }

    /** Each row: scenario, check-in, check-out. */
    @DataProvider(name = "invalid_date_ranges")
    public static Object[][] invalidDateRanges() {
        return new Object[][]{
                {"check-out before check-in", FUTURE_CHECKIN.plusDays(5).toString(), FUTURE_CHECKIN.toString()},
                {"impossible calendar date (30 February)", "2031-02-30", "2031-03-05"},
                {"non-ISO format (MM/DD/YYYY)", "05/01/2031", "05/04/2031"},
                {"not a date at all", "not-a-date", FUTURE_CHECKIN.toString()}};
    }

    /**
     * Zero price (complimentary stay) and same-day check-out (day-use room) could be legitimate, so whether to
     * allow them is a product decision, not a defect (see "Observations" in BUGS.md).
     */
    @DataProvider(name = "open_product_questions")
    public static Object[][] openProductQuestions() {
        return new Object[][]{
                {"zero total price", RequestBuilderBooking.aBooking().totalprice(0).build()},
                {"same-day check-in and check-out", RequestBuilderBooking.aBooking().stayFrom(FUTURE_CHECKIN, FUTURE_CHECKIN).build()}};
    }

    /** Each required field in turn, using dot notation for nested fields. */
    @DataProvider(name = "required_fields")
    public static Object[][] requiredFields() {
        return new Object[][]{{"firstname"}, {"lastname"}, {"totalprice"}, {"depositpaid"},
                {"bookingdates"}, {"bookingdates.checkin"}, {"bookingdates.checkout"}};
    }

    /** Each row: field, value of the wrong type (or blank) to put in it. */
    @DataProvider(name = "wrong_types")
    public static Object[][] wrongTypes() {
        return new Object[][]{
                {"totalprice", "abc"},
                {"depositpaid", "yes"},
                {"firstname", 123},
                {"firstname", ""},
                {"bookingdates", "2031-01-01"}};
    }

    @DataProvider(name = "empty_bodies")
    public static Object[][] emptyBodies() {
        return new Object[][]{{"{}"}, {"[]"}, {""}};
    }

    // ===================== HELPERS =====================

    /** PUT, PATCH and DELETE against a booking; the PATCH sets the given first name. */
    private static List<Named<BookingWriteCall>> writeCallList(String patchedFirstname) {
        BookingWriteCall put = (controller, id, auth) -> controller.updateBooking(id, RequestBuilderBooking.aBooking().build(), auth);
        BookingWriteCall patch = (controller, id, auth) -> controller.patchBooking(id,
                PatchBookingReqPayload.builder().firstname(patchedFirstname).build(), auth);
        BookingWriteCall delete = (controller, id, auth) -> controller.deleteBooking(id, auth);
        return List.of(named("PUT", put), named("PATCH", patch), named("DELETE", delete));
    }
}
