# Reservation Hub: Bookings API regression suite

An automated safety net for the partner-facing Bookings API (create, read, amend, cancel), built after a booking
with a negative total and check-out before check-in reached production.
**Stack:** Java 17+, Maven, TestNG, REST Assured, JSON Schema, Allure (plus Extent for local runs).

- **Test report:** [view online](https://itsdhruv26.github.io/reservation-hub-api-tests/report/), or open [`report/index.html`](report/index.html) locally (no server needed)
- **Bug report:** [`BUGS.md`](BUGS.md): 12 defects with severity and curl repros
- **Latest run:** 75 tests, **33 pass, 42 fail**. Every failure is a real API defect logged in BUGS.md.

## Running it

Needs JDK 17+ (`mvn -v` shows the JDK Maven uses) and, in an IDE, the Lombok plugin.

```bash
mvn clean test                                                # full regression (TestNGFiles/qa/booking_regression.xml)
mvn allure:report                                             # rebuild report/index.html from that run
mvn clean test -DSuiteFile=TestNGFiles/qa/booking_smoke.xml   # 4-test gate: is the core journey alive?
mvn clean test -DSuiteFile=TestNGFiles/misc/AuthTest.xml      # one test class
mvn clean test -DexcludedGroups=known-defect -Dmaven.test.failure.ignore=false   # green CI gate
mvn clean test -Denv=qa -DbaseUrl=https://staging.example.com # pick an environment, override any config key
```

Keep the `clean`: Allure reads every result in `target/allure-results`, so a report built without it mixes in
earlier runs. Each run also writes an Extent report to `test-output/SparkReport/Index.html`.

## Project layout

| Where | What |
|---|---|
| `src/main/java/common` | Generic framework: `SpecBuilder` (base URL, timeouts, retry, report filters), `RestResourceService` (the only class that sends requests), config and YAML loaders, assertions, TestNG listeners, Extent reporting |
| `src/main/java/com/reservationhub` | `applicationapi` (one method per endpoint, no assertions) and `pojo` (request and response payloads) |
| `src/main/resources` | `config/{env}.properties` (URLs, timeouts, retries), `yml/{env}.yml` (credentials), JSON schemas |
| `src/test/java/com/reservationhub` | `controllers` (endpoint calls plus steps like `givenExistingBooking`), `requestbuilder` (valid unique bookings, deliberately broken payloads), `dataprovider`, `core` (`BaseTest`, cold-start check, cleanup, tokens), `testmodules` (the tests) |
| `TestNGFiles` | `qa/`: regression and smoke suites; `misc/`: one suite per test class |

The environment is chosen with `-Denv` (default `qa`). Any config key can be overridden with `-D` or an
environment variable in upper snake case (`baseUrl` → `BASE_URL`).

## Test strategy

**Priority follows partner risk:**
1. **The incident and its siblings:** price and date validation on create, **PUT and PATCH**. Fixing create alone
   would leave the same hole open on amendment, and the suite shows it is open there too.
2. **Silent data corruption:** the API says 200 but stores something else (rolled-over dates, truncated prices,
   PATCH wiping check-in). Worse than an error, because nobody notices.
3. **Core journey and auth:** create → read → PUT → PATCH → delete end to end, plus every write method × every
   bad credential, checking the request is rejected **and** the booking is unchanged.
4. **Contract:** JSON Schema on success responses, documented filter semantics, status codes.

**Tests assert what a correct API should do.** Where the API is wrong, the test fails, is in the `known-defect`
group (a filterable tag in the report) and links to its BUGS.md entry. Excluding that group gives a green gate that still catches *new*
regressions. Boundaries are data-driven and tested from both sides: price -1 is rejected, price 1 and a
one-night stay must still be accepted. In the report, product defects show as *failed* and setup problems as
*broken*, so "the API is wrong" can't be confused with "the test couldn't run".

**Deliberately left out:** XML payloads (partners use JSON); seeded data and pagination (reset underneath us);
fuzzing, injection and rate limits (need their own scope and an environment we may hammer); real load testing.

**Judgement calls:** unauthenticated writes may return 401 or 403, since both deny access. Zero price and
same-day stays could be legitimate (complimentary or day-use), so they are open product questions: a test pins
today's behaviour (accepted, stored as sent) rather than calling them bugs.

## Shared, self-resetting environment and flakiness

- **Self-contained data.** Each test creates its own booking (random names, future dates) and deletes it
  afterwards, pass or fail, including bookings the API wrongly accepted. "Not found" tests use an id they just
  deleted, never a magic number.
- **Order independence is enforced:** tests run in random order every build (seed printed for reruns).
- **Cold start:** a `@BeforeSuite` check polls `/ping` for up to 120 s. If the API never wakes, the run stops
  with one clear message instead of 70 timeouts.
- **Narrow retries:** only 502/503/504 and dropped connections, only on idempotent calls. Never `500` (a real
  defect here) and never `POST /booking` (a retry could double-book).
- **Broken tests get one rerun.** A test that ends with anything but a failed assertion (a read timeout, a
  precondition the sandbox couldn't satisfy) is rerun once, and the report shows the retry. A failed assertion
  is a finding and is never rerun, so this can't hide a defect.
- **Fresh token per test**, so the 10-minute reset can't leave a stale one.

## Known limitations

- The reset can still land mid-test and cause a read-back 404. Rare (tests take 1–2 s); a rerun clears it.
- Tests run sequentially (~3 min) to be polite to a free shared instance.
- The performance check (10 concurrent creates: distinct ids, no cross-talk, p95 < 5 s) is indicative only.
  At scale I'd measure p95/p99 per endpoint, error rate and throughput under sustained load with k6 or Gatling.
- Schemas are hand-written from the docs; with an OpenAPI spec they would be generated so they can't drift.
