package com.reservationhub.controllers.apicontroller;

import com.reservationhub.applicationapi.BookingFlowRequests;
import com.reservationhub.core.tokenmanager.AuthScheme;
import com.reservationhub.core.tokenmanager.BookingTokenManager;
import com.reservationhub.core.utils.BookingCleanup;
import com.reservationhub.core.utils.LatencyStats;
import com.reservationhub.dataprovider.BookingWriteCall;
import com.reservationhub.pojo.booking.BookingPayload;
import com.reservationhub.pojo.createbooking.response.CreateBookingRespPayload;
import com.reservationhub.pojo.getbookingids.response.BookingIdRespPayload;
import com.reservationhub.pojo.patchbooking.request.PatchBookingReqPayload;
import com.reservationhub.requestbuilder.RequestBuilderBooking;
import com.reservationhub.requestbuilder.RequestBuilderBookingSearch;
import com.reservationhub.requestbuilder.RequestBuilderPatchBooking;
import common.core.api.Auth;
import common.core.utils.ConfigLoader;
import common.exception.FrameworkException;
import common.template.HttpReqObject;
import io.qameta.allure.Allure;
import io.restassured.response.Response;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;

import static com.reservationhub.utilities.ReservationHubConstants.BOOKING_CREATED_SCHEMA;
import static com.reservationhub.utilities.ReservationHubConstants.BOOKING_ID_LIST_SCHEMA;
import static com.reservationhub.utilities.ReservationHubConstants.BOOKING_SCHEMA;
import static com.reservationhub.utilities.ReservationHubConstants.DELETE_SUCCESS_STATUSES;
import static com.reservationhub.utilities.ReservationHubConstants.PING_HEALTHY_STATUS;
import static common.core.utils.Assertions.assertMatchesSchema;
import static common.core.utils.Assertions.assertStatusCode;
import static common.core.utils.Assertions.assertStatusCodeIn;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Everything the /booking tests do, so a test method is a single call to one of the scenarios here.
 * <ul>
 *   <li>Endpoint calls (create, get, update, ...) return the raw {@link Response}. Their body parameter is
 *       Object, so negative scenarios can also send what a POJO can't represent (wrong types, missing fields,
 *       malformed JSON).</li>
 *   <li>Scenarios ({@code verify...}), one per test: build the request, make the call and assert what a
 *       correct API must do, in business terms.</li>
 *   <li>Private setup steps ({@code given...}) and checks ({@code assert...}) that the scenarios share.</li>
 * </ul>
 * Write calls without an {@link Auth} argument authenticate as the configured user with a token, the way a
 * partner would; the overloads that take one are for scenarios about credentials.
 * Every booking the API creates through this controller is tracked and deleted after the test, including
 * bookings it wrongly accepted.
 */
public class BookingApiController {

    /** A price with decimals, to check that money is not silently rounded. */
    private static final double DECIMAL_PRICE = 149.99;

    // ─── Endpoint calls ───

    public Response createBooking(Object body) {
        Response response = BookingFlowRequests.createBooking(HttpReqObject.builder().requestBody(body).build());
        BookingCleanup.trackIfCreated(response);
        return response;
    }

    /** Content-negotiation probe: replaces the default Accept header with the given value. */
    public Response createBooking(BookingPayload booking, String acceptHeader) {
        Response response = BookingFlowRequests.createBooking(HttpReqObject.builder()
                .requestBody(booking).accept(acceptHeader).build());
        BookingCleanup.trackIfCreated(response);
        return response;
    }

    /** id is Object so scenarios can also probe non-numeric path values. */
    public Response getBooking(Object id) {
        return BookingFlowRequests.getBooking(HttpReqObject.builder().pathParams(Map.of("id", id)).build());
    }

    public Response searchBookings(Map<String, ?> filters) {
        return BookingFlowRequests.getBookingIds(HttpReqObject.builder().queryParams(filters).build());
    }

    public Response updateBooking(int id, Object body) {
        return updateBooking(id, body, BookingTokenManager.getTokenAuth());
    }

    public Response updateBooking(int id, Object body, Auth auth) {
        return BookingFlowRequests.updateBooking(HttpReqObject.builder()
                .pathParams(Map.of("id", id)).requestBody(body).auth(auth).build());
    }

    public Response patchBooking(int id, PatchBookingReqPayload patch) {
        return patchBooking(id, patch, BookingTokenManager.getTokenAuth());
    }

    public Response patchBooking(int id, PatchBookingReqPayload patch, Auth auth) {
        return BookingFlowRequests.partialUpdateBooking(HttpReqObject.builder()
                .pathParams(Map.of("id", id)).requestBody(patch).auth(auth).build());
    }

    public Response deleteBooking(int id) {
        return deleteBooking(id, BookingTokenManager.getTokenAuth());
    }

    public Response deleteBooking(int id, Auth auth) {
        return BookingFlowRequests.deleteBooking(HttpReqObject.builder()
                .pathParams(Map.of("id", id)).auth(auth).build());
    }

    public Response ping() {
        return BookingFlowRequests.healthCheck(HttpReqObject.builder().build());
    }

    // ─── Scenarios: health ───

    public void verifyHealthy() {
        assertStatusCode(ping(), PING_HEALTHY_STATUS, "GET /ping should report the service as healthy (documented as 201 Created)");
    }

    // ─── Scenarios: end-to-end journey ───

    /** Create, read, replace, amend and cancel one booking, each as its own step in the reports. */
    public void verifyFullBookingLifecycle() {
        BookingPayload original = RequestBuilderBooking.genBooking();
        BookingPayload replacement = RequestBuilderBooking.genReplacementFor(original);
        PatchBookingReqPayload patch = RequestBuilderPatchBooking.genAdditionalNeedsPatch("Airport transfer");

        int id = Allure.step("Create booking", () -> assertCreated(createBooking(original), original));
        Allure.step("Read it back", () -> assertPersisted(id, original, "A created booking should be stored as sent"));
        Allure.step("Replace it (PUT)", () -> assertReplaced(updateBooking(id, replacement), id, replacement));
        Allure.step("Amend one field (PATCH)", () -> assertPatched(patchBooking(id, patch), id, replacement, patch));
        Allure.step("Cancel it (DELETE)", () -> assertCancelled(deleteBooking(id), id, replacement));
    }

    // ─── Scenarios: create ───

    public void verifyValidBookingCreated() {
        BookingPayload booking = RequestBuilderBooking.genBooking();
        assertCreated(createBooking(booking), booking);
    }

    public void verifyCreatedBookingRetrievable() {
        CreateBookingRespPayload created = givenExistingBooking();

        Response response = getBooking(created.getBookingid());

        assertStatusCode(response, 200, "The booking should be retrievable");
        assertMatchesSchema(response, BOOKING_SCHEMA);
        assertThat(response.as(BookingPayload.class)).isEqualTo(created.getBooking());
    }

    public void verifyBookingWithoutAdditionalNeedsCreated() {
        BookingPayload booking = RequestBuilderBooking.genBookingWithoutAdditionalNeeds();
        assertCreated(createBooking(booking), booking);
    }

    public void verifyCreateAcceptsAcceptHeader(String acceptHeader) {
        Response response = createBooking(RequestBuilderBooking.genBooking(), acceptHeader);

        assertStatusCode(response, 200, "Any Accept header that admits application/json should get a JSON response");
    }

    public void verifyDecimalPricePreserved() {
        Response response = createBooking(RequestBuilderBooking.genPayloadWith("totalprice", DECIMAL_PRICE));

        assertStatusCode(response, 200, "A booking priced " + DECIMAL_PRICE + " should be accepted");
        // Read with jsonPath, not the BookingPayload POJO: its Integer totalprice would hide the decimals we are checking for.
        assertThat(response.jsonPath().getDouble("booking.totalprice"))
                .as("Stored price must equal the price the partner sent (no silent rounding of money)")
                .isEqualTo(DECIMAL_PRICE);
    }

    // ─── Scenarios: input validation on create ───

    public void verifyIncidentBookingRejected() {
        assertRejected(createBooking(RequestBuilderBooking.genIncidentBooking()),
                "negative total price and check-out before check-in");
    }

    public void verifyNegativePriceRejected(int price) {
        assertRejected(createBooking(RequestBuilderBooking.genBookingWithPrice(price)), "totalprice " + price);
    }

    /** {@code scenario} names the date problem in the failure message, e.g. "check-out before check-in". */
    public void verifyInvalidDatesRejected(String scenario, String checkin, String checkout) {
        assertRejected(createBooking(RequestBuilderBooking.genBookingWithDates(checkin, checkout)), scenario);
    }

    /** Guards the other side of the boundary: tightening validation must not reject legitimate bookings. */
    public void verifyMinimumValidBookingAccepted() {
        assertStatusCode(createBooking(RequestBuilderBooking.genMinimumValidBooking()), 200,
                "The smallest legitimate booking must still be accepted");
    }

    /** Accepted (pending a product decision) and stored exactly as sent, not silently altered. */
    public void verifyAcceptedAndStoredAsSent(String scenario, BookingPayload booking) {
        Response response = createBooking(booking);

        assertStatusCode(response, 200, "Booking with " + scenario + " is currently allowed (pending a product decision)");
        assertPersisted(response.as(CreateBookingRespPayload.class).getBookingid(), booking,
                "If accepted, it must be stored exactly as sent, not silently altered");
    }

    /** {@code field} in dot notation for nested fields, e.g. "bookingdates.checkin". */
    public void verifyMissingFieldRejected(String field) {
        assertRejected(createBooking(RequestBuilderBooking.genPayloadWithout(field)), "missing " + field);
    }

    public void verifyWrongTypeRejected(String field, Object value) {
        assertRejected(createBooking(RequestBuilderBooking.genPayloadWith(field, value)), field + " = " + value);
    }

    public void verifyEmptyBodyRejected(String body) {
        assertRejected(createBooking(body), "body " + body);
    }

    public void verifyMalformedJsonRejected() {
        assertRejected(createBooking(RequestBuilderBooking.genMalformedJson()), "truncated JSON");
    }

    // ─── Scenarios: search ───

    public void verifyBookingIdListMatchesContract() {
        Response response = searchBookings(RequestBuilderBookingSearch.genNoFilters());

        assertStatusCode(response, 200, "GET /booking should list bookings");
        assertMatchesSchema(response, BOOKING_ID_LIST_SCHEMA);
    }

    /** Both names match: exactly this booking. Only the first name matches: not this booking. */
    public void verifyNameFilterFindsOnlyMatchingBooking() {
        CreateBookingRespPayload created = givenExistingBooking();
        Map<String, String> bothNames = RequestBuilderBookingSearch.genNameFilter(created.getBooking());
        Map<String, String> firstNameOnly = RequestBuilderBookingSearch.genNameFilter(created.getBooking().getFirstname(), "Someone-else");

        assertThat(searchBookingIds(bothNames))
                .as("Search with %s should return exactly booking %d", bothNames, created.getBookingid())
                .containsExactly(created.getBookingid());
        assertSearchFinds(firstNameOnly, created, false);
    }

    /** The filter date is set {@code offsetDays} from the booking's own check-in or check-out date. */
    public void verifyDateFilterBoundary(String filter, int offsetDays, boolean expectIncluded) {
        CreateBookingRespPayload created = givenExistingBooking();

        assertSearchFinds(RequestBuilderBookingSearch.genDateFilterAround(created.getBooking(), filter, offsetDays),
                created, expectIncluded);
    }

    public void verifyMalformedDateFilterRejected() {
        Response response = searchBookings(RequestBuilderBookingSearch.genCheckinFilter("not-a-date"));

        assertStatusCode(response, 400, "An unparseable filter is a client error and should get 400, not a server crash");
    }

    // ─── Scenarios: update (PUT) ───

    public void verifyPutReplacesBooking() {
        CreateBookingRespPayload existing = givenExistingBooking();
        BookingPayload replacement = RequestBuilderBooking.genReplacementFor(existing.getBooking());

        assertReplaced(updateBooking(existing.getBookingid(), replacement), existing.getBookingid(), replacement);
    }

    public void verifyPutWithoutAdditionalNeedsClearsIt() {
        CreateBookingRespPayload existing = givenExistingBooking(RequestBuilderBooking.genBookingWithAdditionalNeeds("Breakfast"));
        BookingPayload replacement = RequestBuilderBooking.genReplacementWithoutAdditionalNeeds(existing.getBooking());

        Response response = updateBooking(existing.getBookingid(), replacement);

        assertStatusCode(response, 200, "Full update should succeed");
        assertThat(fetchBooking(existing.getBookingid()).getAdditionalneeds())
                .as("PUT is a full replacement: a field left out of the request must not survive")
                .isNull();
    }

    /** {@code corruption} turns the existing booking into an invalid replacement. */
    public void verifyInvalidPutRejected(UnaryOperator<BookingPayload> corruption) {
        CreateBookingRespPayload existing = givenExistingBooking();

        Response response = updateBooking(existing.getBookingid(), corruption.apply(existing.getBooking()));

        assertRejectedAndUnchanged(response, existing, "An update must be validated like a create");
    }

    public void verifyIncompletePutRejected() {
        CreateBookingRespPayload existing = givenExistingBooking();

        Response response = updateBooking(existing.getBookingid(), RequestBuilderBooking.genIncompletePayload());

        assertRejectedAndUnchanged(response, existing, "PUT requires the full booking; partial changes belong in PATCH");
    }

    // ─── Scenarios: amend (PATCH) ───

    public void verifyPatchChangesOnlyGivenFields() {
        CreateBookingRespPayload existing = givenExistingBooking();
        PatchBookingReqPayload patch = RequestBuilderPatchBooking.genRenameAndRepricePatch(existing.getBooking());

        assertPatched(patchBooking(existing.getBookingid(), patch), existing.getBookingid(), existing.getBooking(), patch);
    }

    /** A patch that sends only the nested check-out must leave the stored check-in untouched. */
    public void verifyExtendingStayKeepsCheckin() {
        CreateBookingRespPayload existing = givenExistingBooking();
        PatchBookingReqPayload patch = RequestBuilderPatchBooking.genExtendStayPatch(existing.getBooking(), 2);

        Response response = patchBooking(existing.getBookingid(), patch);

        assertStatusCode(response, 200, "Extending a stay should succeed");
        assertThat(fetchBooking(existing.getBookingid()).getBookingdates())
                .as("Only check-out was sent, so check-in must be untouched")
                .isEqualTo(RequestBuilderBooking.genExpectedAfterPatch(existing.getBooking(), patch).getBookingdates());
    }

    public void verifyInvalidPatchRejected(PatchBookingReqPayload patch) {
        CreateBookingRespPayload existing = givenExistingBooking();

        Response response = patchBooking(existing.getBookingid(), patch);

        assertRejectedAndUnchanged(response, existing, "An amendment must be validated like a create");
    }

    // ─── Scenarios: cancel (DELETE) ───

    public void verifyCancelRemovesBooking() {
        CreateBookingRespPayload existing = givenExistingBooking();

        assertCancelled(deleteBooking(existing.getBookingid()), existing.getBookingid(), existing.getBooking());
    }

    public void verifyCancelReturnsConventionalStatus() {
        CreateBookingRespPayload existing = givenExistingBooking();

        assertStatusCodeIn(deleteBooking(existing.getBookingid()),
                "201 Created means a resource was created; a deletion should return 200 or 204", 200, 204);
    }

    // ─── Scenarios: auth on writes ───

    /**
     * Denied, and the stored booking is exactly as it was. 401 is the textbook answer and the API sends 403;
     * both deny access, which is the risk that matters here.
     */
    public void verifyWriteWithoutValidCredentialsDenied(BookingWriteCall write, Auth badAuth) {
        CreateBookingRespPayload existing = givenExistingBooking();

        Response response = write.send(this, existing.getBookingid(), badAuth);

        assertStatusCodeIn(response, "A write without valid credentials must be denied", 401, 403);
        assertPersisted(existing.getBookingid(), existing.getBooking(), "A rejected write must not modify the booking");
    }

    public void verifyAuthSchemeAcceptedForWrites(AuthScheme scheme) {
        CreateBookingRespPayload existing = givenExistingBooking();
        PatchBookingReqPayload patch = RequestBuilderPatchBooking.genFirstnamePatch("Renamed");

        Response response = patchBooking(existing.getBookingid(), patch, scheme.toAuth());

        assertPatched(response, existing.getBookingid(), existing.getBooking(), patch);
    }

    // ─── Scenarios: not found ───

    public void verifyDeletedBookingNotFound() {
        assertStatusCode(getBooking(givenDeletedBookingId()), 404, "A deleted booking should be reported as not found");
    }

    public void verifyNonNumericIdNotFound() {
        assertStatusCode(getBooking("not-an-id"), 404, "A non-numeric id cannot match a booking");
    }

    public void verifyWriteToDeletedBookingNotFound(BookingWriteCall write) {
        Response response = write.send(this, givenDeletedBookingId());

        assertStatusCode(response, 404, "Amending or cancelling a booking that doesn't exist should say 'not found'");
    }

    // ─── Scenarios: performance ───

    /**
     * A burst of concurrent creates (perfConcurrentCreates): every one succeeded, got its own id and kept its
     * own data, and p95 latency is under perfP95ThresholdMs. The latency summary is attached to the reports
     * first, so it is there even when a check fails.
     */
    public void verifyConcurrentCreates() {
        int count = ConfigLoader.getInstance().getPerfConcurrentCreates();

        List<TimedCreate> results = createConcurrently(count);

        LatencyStats.attachSummary(results);
        assertThat(results).as("Every concurrent create should succeed").allMatch(r -> r.status() == 200);
        assertThat(results.stream().map(TimedCreate::bookingId).distinct().count())
                .as("Every booking should get its own id").isEqualTo((long) count);
        assertThat(results).as("Each response should carry the booking that was sent, not another request's data")
                .allMatch(r -> r.sent().equals(r.stored()));
        assertThat(LatencyStats.p95(results))
                .as("p95 latency (ms) for a create on the shared sandbox")
                .isLessThan(ConfigLoader.getInstance().getPerfP95ThresholdMs());
    }

    /** Outcome of one create in a concurrent burst; bookingId and stored are null if the create failed. */
    public record TimedCreate(BookingPayload sent, int status, Integer bookingId, BookingPayload stored, long latencyMs) {
    }

    // ─── Setup steps ───

    private CreateBookingRespPayload givenExistingBooking() {
        return givenExistingBooking(RequestBuilderBooking.genBooking());
    }

    /**
     * Setup failures throw FrameworkException rather than an assertion error, so the reports mark the test
     * "broken" (couldn't reach a verdict) instead of "failed" (product defect).
     */
    private CreateBookingRespPayload givenExistingBooking(BookingPayload booking) {
        return Allure.step("Given an existing booking", () -> {
            Response response = createBooking(booking);
            if (response.statusCode() != 200) {
                throw new FrameworkException("Precondition failed: could not create booking. HTTP "
                        + response.statusCode() + ", body: " + response.asString());
            }
            return response.as(CreateBookingRespPayload.class);
        });
    }

    /**
     * An id that is known not to exist: a booking created and then deleted here. Safer than guessing an id
     * that "probably" doesn't exist on a shared instance.
     */
    private int givenDeletedBookingId() {
        int id = givenExistingBooking().getBookingid();
        Response deletion = deleteBooking(id, BookingTokenManager.getBasicAuth());
        if (deletion.statusCode() >= 300) {
            throw new FrameworkException("Precondition failed: could not delete booking " + id
                    + ". HTTP " + deletion.statusCode());
        }
        return id;
    }

    /** Reads a booking back from the API: the source of truth for "was the change actually persisted?". */
    private BookingPayload fetchBooking(int id) {
        return Allure.step("Read booking " + id + " back from the API", () -> {
            Response response = getBooking(id);
            assertStatusCode(response, 200, "Booking " + id + " should be retrievable");
            return response.as(BookingPayload.class);
        });
    }

    /** Ids returned by GET /booking for the given filters, e.g. {@code Map.of("firstname", "Jim")}. */
    private List<Integer> searchBookingIds(Map<String, ?> filters) {
        Response response = searchBookings(filters);
        assertStatusCode(response, 200, "Search with filters " + filters + " should succeed");
        return Arrays.stream(response.as(BookingIdRespPayload[].class)).map(BookingIdRespPayload::getBookingid).toList();
    }

    // ─── Checks shared by the scenarios ───

    /** Accepted, in the documented shape, and the response echoes exactly what was booked. Returns the new id. */
    private int assertCreated(Response response, BookingPayload sent) {
        assertStatusCode(response, 200, "A valid booking should be accepted");
        assertMatchesSchema(response, BOOKING_CREATED_SCHEMA);
        CreateBookingRespPayload created = response.as(CreateBookingRespPayload.class);
        assertThat(created.getBooking()).as("The response should echo exactly what was booked").isEqualTo(sent);
        return created.getBookingid();
    }

    /** {@code what} completes "Booking with ... should be rejected", e.g. "totalprice -1". */
    private void assertRejected(Response response, String what) {
        assertStatusCode(response, 400, "Booking with " + what + " should be rejected as a client error");
    }

    /** Reads the booking back: what the API stored must equal {@code expected}. */
    private void assertPersisted(int id, BookingPayload expected, String expectation) {
        assertThat(fetchBooking(id)).as(expectation).isEqualTo(expected);
    }

    /** Searching with {@code filters} does, or does not, return this booking. */
    private void assertSearchFinds(Map<String, ?> filters, CreateBookingRespPayload booking, boolean expectFound) {
        List<Integer> ids = searchBookingIds(filters);
        String search = String.format("Booking with %s searched with %s", booking.getBooking().getBookingdates(), filters);
        if (expectFound) {
            assertThat(ids).as(search + " should be found").contains(booking.getBookingid());
        } else {
            assertThat(ids).as(search + " should NOT be found").doesNotContain(booking.getBookingid());
        }
    }

    /** Accepted, the response returns the replacement in the documented shape, and the replacement is stored. */
    private void assertReplaced(Response response, int id, BookingPayload replacement) {
        assertStatusCode(response, 200, "A valid full update should succeed");
        assertMatchesSchema(response, BOOKING_SCHEMA);
        assertThat(response.as(BookingPayload.class)).as("Response should return the updated booking").isEqualTo(replacement);
        assertPersisted(id, replacement, "Update should be persisted");
    }

    /** Accepted, and both the response and the stored booking are {@code before} with only the patched fields changed. */
    private void assertPatched(Response response, int id, BookingPayload before, PatchBookingReqPayload patch) {
        BookingPayload expected = RequestBuilderBooking.genExpectedAfterPatch(before, patch);
        assertStatusCode(response, 200, "A valid partial update should succeed");
        assertThat(response.as(BookingPayload.class)).as("Response should return the amended booking").isEqualTo(expected);
        assertPersisted(id, expected, "Only the patched fields should change");
    }

    /** Rejected as a client error, and the stored booking is exactly as it was. */
    private void assertRejectedAndUnchanged(Response response, CreateBookingRespPayload existing, String expectation) {
        assertStatusCode(response, 400, expectation);
        assertPersisted(existing.getBookingid(), existing.getBooking(), "A rejected change must not modify the booking");
    }

    /**
     * The cancellation succeeded and the booking is gone from lookup and search. Any success status passes
     * here; the exact code is checked by {@link #verifyCancelReturnsConventionalStatus()}.
     */
    private void assertCancelled(Response response, int id, BookingPayload booking) {
        assertStatusCodeIn(response, "Cancellation should succeed", DELETE_SUCCESS_STATUSES);
        assertStatusCode(getBooking(id), 404, "A cancelled booking should not be retrievable");
        assertThat(searchBookingIds(RequestBuilderBookingSearch.genNameFilter(booking)))
                .as("A cancelled booking should not appear in search results")
                .doesNotContain(id);
    }

    // ─── Concurrency ───

    /**
     * Sends {@code count} valid creates at once, one thread each, and times every call. The bookings are
     * tracked for cleanup here, on the calling thread, because tracking is per thread.
     */
    private List<TimedCreate> createConcurrently(int count) {
        ExecutorService pool = Executors.newFixedThreadPool(count);
        List<TimedCreate> results;
        try {
            List<CompletableFuture<TimedCreate>> futures = IntStream.range(0, count)
                    .mapToObj(i -> CompletableFuture.supplyAsync(this::timedCreate, pool))
                    .toList();
            results = futures.stream().map(CompletableFuture::join).toList();
        } finally {
            pool.shutdown();
        }
        results.stream().filter(r -> r.bookingId() != null).forEach(r -> BookingCleanup.track(r.bookingId()));
        return results;
    }

    /**
     * Runs on a worker thread, so the call is unreported and untracked: Allure's per-request attachments are
     * not thread-safe, and {@link #createConcurrently} tracks what was created.
     */
    private TimedCreate timedCreate() {
        BookingPayload booking = RequestBuilderBooking.genBooking();
        long start = System.nanoTime();
        Response response = BookingFlowRequests.createBooking(HttpReqObject.builder().requestBody(booking).reported(false).build());
        long latencyMs = (System.nanoTime() - start) / 1_000_000;
        CreateBookingRespPayload created = response.statusCode() == 200 ? response.as(CreateBookingRespPayload.class) : null;
        return new TimedCreate(booking, response.statusCode(),
                created != null ? created.getBookingid() : null,
                created != null ? created.getBooking() : null,
                latencyMs);
    }
}
