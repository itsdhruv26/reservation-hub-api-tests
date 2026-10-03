# Reservation Hub: Bookings API regression suite

An automated safety net for the partner-facing Bookings API (create, read, amend, cancel), built after a booking
with a negative total and check-out before check-in reached production.
**Stack:** Java 17+, Maven, TestNG, REST Assured, JSON Schema, Allure.

- **Test report:** [`report/index.html`](report/index.html) (opens in a browser, no server needed)
- **Bug report:** [`BUGS.md`](BUGS.md): 12 defects with severity and curl repros
- **Latest run:** 75 tests, **33 pass, 42 fail**. Every failure is a real API defect logged in BUGS.md.

## Running it

Needs JDK 17+ (`mvn -v` shows the JDK Maven uses) and, in an IDE, the Lombok plugin.

```bash
mvn test                                    # full suite, then: mvn allure:report
mvn test -Dgroups=smoke                     # 4-test gate: is the core journey alive?
mvn test -DexcludedGroups=known-defect -Dmaven.test.failure.ignore=false   # green CI gate
mvn test -Dbase.url=https://staging.example.com                            # override any config key
```

Config (base URL, credentials, timeouts) lives in `config.properties`; any key can be overridden with `-D` or an
environment variable. Request logic (`controller/`, `http/`), test data (`builder/`, `data/`) and tests (`tests/`)
are kept separate. Maven runs the classes listed in [`testng.xml`](testng.xml).

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
group and links to its BUGS.md entry. Excluding that group gives a green gate that still catches *new*
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
- **Fresh token per test**, so the 10-minute reset can't leave a stale one.

## Known limitations

- The reset can still land mid-test and cause a read-back 404. Rare (tests take 1–2 s); a rerun clears it.
- Tests run sequentially (~3 min) to be polite to a free shared instance.
- The performance check (10 concurrent creates: distinct ids, no cross-talk, p95 < 5 s) is indicative only.
  At scale I'd measure p95/p99 per endpoint, error rate and throughput under sustained load with k6 or Gatling.
- Schemas are hand-written from the docs; with an OpenAPI spec they would be generated so they can't drift.
