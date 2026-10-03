# Bug report: Reservation Hub Bookings API

Found while building the automated suite against `https://restful-booker.herokuapp.com` (October 2026).
Every bug below is covered by at least one automated test tagged `known-defect`, linked by the BUG id
in the Allure report, so we will know as soon as a fix lands.

**Severity scale used**

| Severity | Meaning |
|---|---|
| Critical | Bad data is accepted silently and affects money or stay dates. This is the class of the production incident. |
| High | Valid-looking data is silently changed or lost. The partner gets a success response but a different booking is stored. |
| Medium | The request fails or behaves wrongly in a way the partner *can* see, but it is confusing or breaks integrations. |
| Low | HTTP/REST convention violations with little business impact. |

## Summary

| ID | Title | Severity | Endpoint(s) |
|---|---|---|---|
| [BUG-01](#BUG-01) | Bookings with a negative price or check-out before check-in are accepted | Critical | POST, PUT, PATCH /booking |
| [BUG-02](#BUG-02) | Invalid dates are silently rewritten instead of rejected | High | POST /booking |
| [BUG-03](#BUG-03) | PATCHing only the check-out date destroys the check-in date | High | PATCH /booking/{id} |
| [BUG-04](#BUG-04) | Wrongly typed fields are coerced: a non-numeric price is stored as `null` | High | POST /booking |
| [BUG-05](#BUG-05) | Decimal prices are truncated to whole numbers | High | POST /booking |
| [BUG-06](#BUG-06) | Missing or malformed input crashes the server (500) instead of returning 400 | Medium | POST /booking, GET /booking |
| [BUG-07](#BUG-07) | Date search filters don't match the documented behaviour | Medium | GET /booking |
| [BUG-08](#BUG-08) | PUT does not clear an optional field that was left out | Medium | PUT /booking/{id} |
| [BUG-09](#BUG-09) | Failed login returns 200 OK | Medium | POST /auth |
| [BUG-12](#BUG-12) | Standard `Accept` headers are answered with 418 I'm a Teapot | Medium | POST /booking |
| [BUG-10](#BUG-10) | Writes to a non-existent booking return 405 instead of 404 | Low | PUT, PATCH, DELETE /booking/{id} |
| [BUG-11](#BUG-11) | Successful cancellation returns 201 Created | Low | DELETE /booking/{id} |

All `curl` commands below are self-contained. Write calls use the documented Basic Auth (`admin:password123`)
so you don't need to fetch a token first.

---

<a id="BUG-01"></a>
## BUG-01: Bookings with a negative price or check-out before check-in are accepted

**Severity: Critical.** This is exactly the production incident. Invalid bookings reach partners'
systems and invoices, and nothing tells the partner that anything went wrong.

**Steps to reproduce**

```bash
curl -i -X POST https://restful-booker.herokuapp.com/booking \
  -H 'Content-Type: application/json' -H 'Accept: application/json' \
  -d '{"firstname":"Neg","lastname":"Price","totalprice":-100,"depositpaid":true,
       "bookingdates":{"checkin":"2031-01-10","checkout":"2031-01-05"}}'
```

**Expected:** `400 Bad Request` with a message naming the invalid fields. Nothing is stored.

**Actual:** `200 OK`, and the booking is stored as sent:
```json
{"bookingid":3114,"booking":{"firstname":"Neg","lastname":"Price","totalprice":-100,"depositpaid":true,
 "bookingdates":{"checkin":"2031-01-10","checkout":"2031-01-05"}}}
```

**Also affects updates.** A valid booking can be made invalid afterwards. `PUT` with `"totalprice":-5` and
inverted dates, or `PATCH` with `{"totalprice":-1}`, both return `200` and persist the change. So fixing
validation on create alone would not close the incident.

**Tests:** `BookingValidationTest.incidentBookingIsRejected`, `negativePriceIsRejected`,
`invalidDatesAreRejected[check-out before check-in]`, `UpdateBookingTest.invalidPutIsRejected`,
`PatchBookingTest.invalidPatchIsRejected`

---

<a id="BUG-02"></a>
## BUG-02: Invalid dates are silently rewritten instead of rejected

**Severity: High.** The partner gets `200 OK` but the stored stay is on different dates than the ones
they sent, or has no usable dates at all. Guests would arrive on dates the hotel doesn't expect.

**Steps to reproduce**

```bash
# 30 February does not exist
curl -s -X POST https://restful-booker.herokuapp.com/booking \
  -H 'Content-Type: application/json' -H 'Accept: application/json' \
  -d '{"firstname":"A","lastname":"B","totalprice":100,"depositpaid":true,
       "bookingdates":{"checkin":"2031-02-30","checkout":"2031-03-05"}}'
```

**Expected:** `400 Bad Request` for any check-in or check-out that isn't a real `YYYY-MM-DD` date.

**Actual:** `200 OK`, with the date rewritten:

| Sent `checkin` | Stored `checkin` |
|---|---|
| `2031-02-30` | `2031-03-02` (rolled into March) |
| `05/01/2031` | `2031-05-01` (non-ISO format guessed as US month/day) |
| `not-a-date` | `0NaN-aN-aN` |

**Tests:** `BookingValidationTest.invalidDatesAreRejected`

---

<a id="BUG-03"></a>
## BUG-03: PATCHing only the check-out date destroys the check-in date

**Severity: High.** This is a legitimate everyday amendment (a guest extends their stay), and it
corrupts a valid booking. Unlike the other validation bugs, the partner did nothing wrong.

**Steps to reproduce**

```bash
ID=$(curl -s -X POST https://restful-booker.herokuapp.com/booking \
  -H 'Content-Type: application/json' -H 'Accept: application/json' \
  -d '{"firstname":"Ext","lastname":"Stay","totalprice":300,"depositpaid":true,
       "bookingdates":{"checkin":"2031-06-01","checkout":"2031-06-04"}}' \
  | grep -o '"bookingid":[0-9]*' | cut -d: -f2)

curl -s -X PATCH https://restful-booker.herokuapp.com/booking/$ID -u admin:password123 \
  -H 'Content-Type: application/json' -H 'Accept: application/json' \
  -d '{"bookingdates":{"checkout":"2031-06-06"}}'
```

**Expected:** `200 OK`, with `checkin` still `2031-06-01` and `checkout` now `2031-06-06`.

**Actual:** `200 OK`, but the check-in date is gone:
```json
"bookingdates":{"checkin":"0NaN-aN-aN","checkout":"2031-06-06"}
```
PATCH replaces the whole nested `bookingdates` object instead of merging it, then formats the missing
check-in as a date.

**Tests:** `PatchBookingTest.patchingCheckoutPreservesCheckin`

---

<a id="BUG-04"></a>
## BUG-04: Wrongly typed fields are coerced: a non-numeric price is stored as `null`

**Severity: High.** A booking with no price is stored and confirmed with `200`. Downstream billing
then gets `null`. A boolean field also accepts any truthy string.

**Steps to reproduce**

```bash
curl -s -X POST https://restful-booker.herokuapp.com/booking \
  -H 'Content-Type: application/json' -H 'Accept: application/json' \
  -d '{"firstname":"Jim","lastname":"Brown","totalprice":"abc","depositpaid":"yes",
       "bookingdates":{"checkin":"2031-01-01","checkout":"2031-01-05"}}'
```

**Expected:** `400 Bad Request`: `totalprice` must be a number and `depositpaid` a boolean.

**Actual:** `200 OK` with `"totalprice":null` and `"depositpaid":true`. A blank `"firstname":""` is also
accepted, so a booking can be stored with no guest name.

**Tests:** `BookingValidationTest.wrongTypeIsRejected`

---

<a id="BUG-05"></a>
## BUG-05: Decimal prices are truncated to whole numbers

**Severity: High.** This is direct revenue loss: €149.99 is stored as €149. The API is documented to
accept a *number*, so partners have no reason to expect rounding, and the truncation always goes down.

**Steps to reproduce**

```bash
curl -s -X POST https://restful-booker.herokuapp.com/booking \
  -H 'Content-Type: application/json' -H 'Accept: application/json' \
  -d '{"firstname":"Dec","lastname":"Price","totalprice":149.99,"depositpaid":true,
       "bookingdates":{"checkin":"2031-01-01","checkout":"2031-01-05"}}'
```

**Expected:** stored and returned `totalprice` is `149.99`. If only integers are allowed, the API should
reject decimals with `400`, not truncate them.

**Actual:** `200 OK`, `"totalprice":149`.

**Tests:** `CreateBookingTest.decimalPriceIsPreserved`

---

<a id="BUG-06"></a>
## BUG-06: Missing or malformed input crashes the server (500) instead of returning 400

**Severity: Medium.** No bad data is stored, so it isn't High. But the partner gets
`Internal Server Error` with no hint of what to fix. It looks like an outage on our side, so it creates
support tickets, and 5xx responses set off monitoring alerts and partner retries.

**Steps to reproduce**

```bash
# Required field missing (same result for lastname, totalprice, depositpaid, bookingdates, checkin, checkout)
curl -i -X POST https://restful-booker.herokuapp.com/booking \
  -H 'Content-Type: application/json' -H 'Accept: application/json' \
  -d '{"lastname":"Brown","totalprice":111,"depositpaid":true,
       "bookingdates":{"checkin":"2031-01-01","checkout":"2031-01-05"}}'

# Unparseable search filter
curl -i 'https://restful-booker.herokuapp.com/booking?checkin=not-a-date'
```

**Expected:** `400 Bad Request` with a body naming the missing or invalid field.

**Actual:** `500 Internal Server Error`, body `Internal Server Error`. The same happens for `{}`, `[]`,
an empty body, `"firstname":123`, and `"bookingdates":"2031-01-01"`. (Truncated JSON correctly gets `400`,
so the parser is fine and the validation layer is what's missing.)

**Tests:** `BookingValidationTest.missingRequiredFieldIsRejected`, `emptyBodyIsRejected`,
`wrongTypeIsRejected[firstname=123, bookingdates]`, `GetBookingTest.malformedDateFilterIsRejected`

---

<a id="BUG-07"></a>
## BUG-07: Date search filters don't match the documented behaviour

**Severity: Medium.** Partners using these filters for reconciliation, such as "all arrivals from
today", silently miss bookings. The response is a valid-looking list, so nothing alerts them.

The [docs](https://restful-booker.herokuapp.com/apidoc/index.html#api-Booking-GetBookings) say both
filters mean "on or after": `checkin` should return bookings with check-in >= the date, and `checkout`
should return bookings with check-out >= the date.

**Steps to reproduce**

```bash
NAME=Filter$RANDOM
curl -s -X POST https://restful-booker.herokuapp.com/booking \
  -H 'Content-Type: application/json' -H 'Accept: application/json' \
  -d "{\"firstname\":\"$NAME\",\"lastname\":\"Zed\",\"totalprice\":100,\"depositpaid\":true,
       \"bookingdates\":{\"checkin\":\"2031-03-10\",\"checkout\":\"2031-03-15\"}}"

curl -s "https://restful-booker.herokuapp.com/booking?firstname=$NAME&checkin=2031-03-10"   # expected: found
curl -s "https://restful-booker.herokuapp.com/booking?firstname=$NAME&checkout=2031-03-14"  # expected: found
curl -s "https://restful-booker.herokuapp.com/booking?firstname=$NAME&checkout=2031-03-16"  # expected: not found
```

**Expected vs actual** (booking: check-in 10 Mar, check-out 15 Mar)

| Filter | Expected (per docs) | Actual |
|---|---|---|
| `checkin=2031-03-10` (same day) | included | **excluded**: the filter is strictly "after" |
| `checkout=2031-03-14` | included | **excluded** |
| `checkout=2031-03-16` | excluded | **included**: the filter behaves as "on or before" |

**Tests:** `GetBookingTest.dateFilterBoundary`

---

<a id="BUG-08"></a>
## BUG-08: PUT does not clear an optional field that was left out

**Severity: Medium.** A partner who removes "Breakfast" from a booking with a full update still has it
on the booking, so the guest may be served and charged for it. A workaround exists (send an empty string),
but nothing tells the partner they need it.

**Steps to reproduce**

```bash
ID=$(curl -s -X POST https://restful-booker.herokuapp.com/booking \
  -H 'Content-Type: application/json' -H 'Accept: application/json' \
  -d '{"firstname":"Put","lastname":"Test","totalprice":100,"depositpaid":true,
       "bookingdates":{"checkin":"2031-01-01","checkout":"2031-01-05"},"additionalneeds":"Breakfast"}' \
  | grep -o '"bookingid":[0-9]*' | cut -d: -f2)

curl -s -X PUT https://restful-booker.herokuapp.com/booking/$ID -u admin:password123 \
  -H 'Content-Type: application/json' -H 'Accept: application/json' \
  -d '{"firstname":"Put","lastname":"Test","totalprice":100,"depositpaid":true,
       "bookingdates":{"checkin":"2031-01-01","checkout":"2031-01-05"}}'
```

**Expected:** PUT replaces the whole resource, so `additionalneeds` is no longer present.

**Actual:** `200 OK`, and the response and stored booking still contain `"additionalneeds":"Breakfast"`.

**Tests:** `UpdateBookingTest.putWithoutOptionalFieldClearsIt`

---

<a id="BUG-09"></a>
## BUG-09: Failed login returns 200 OK

**Severity: Medium.** Integrations that check the status code (the normal pattern) treat a failed
login as success. Their next write then fails with a confusing 403 far from the real cause. There is no
direct security impact, since no token is issued.

**Steps to reproduce**

```bash
curl -i -X POST https://restful-booker.herokuapp.com/auth \
  -H 'Content-Type: application/json' -d '{"username":"admin","password":"wrong"}'
```

**Expected:** `401 Unauthorized`.

**Actual:** `200 OK` with body `{"reason":"Bad credentials"}`. Same for an unknown user and an empty body.

**Tests:** `AuthTest.invalidCredentialsAreRejected`

---

<a id="BUG-12"></a>
## BUG-12: Standard `Accept` headers are answered with 418 I'm a Teapot

**Severity: Medium.** Booking creation only works if the client sends exactly `Accept: application/json`
or `*/*`. Many HTTP clients send a list by default; axios, for example, sends `application/json, text/plain, */*`.
We hit this ourselves: REST Assured's default JSON header got `418` on every create. Each new partner
integration is likely to run into it.

**Steps to reproduce**

```bash
curl -i -X POST https://restful-booker.herokuapp.com/booking \
  -H 'Content-Type: application/json' -H 'Accept: application/json, text/plain, */*' \
  -d '{"firstname":"A","lastname":"B","totalprice":1,"depositpaid":true,
       "bookingdates":{"checkin":"2031-01-01","checkout":"2031-01-05"}}'
```

**Expected:** `200 OK` with JSON. The header explicitly allows `application/json`. If no acceptable type
existed, the correct status would be `406 Not Acceptable`.

**Actual:** `418 I'm a Teapot`. Also for `application/json;q=0.9` and for a request with no `Accept` header.

**Tests:** `CreateBookingTest.createAcceptsStandardAcceptHeaders`

---

<a id="BUG-10"></a>
## BUG-10: Writes to a non-existent booking return 405 instead of 404

**Severity: Low.** The write is rejected either way. But "Method Not Allowed" suggests the partner's
code is wrong, when really the booking is gone (already cancelled, for example).

**Steps to reproduce**

```bash
curl -i -X DELETE https://restful-booker.herokuapp.com/booking/999999999 -u admin:password123
```

**Expected:** `404 Not Found` for PUT, PATCH and DELETE on an id that doesn't exist. A repeated DELETE is a
common retry pattern, and it should also give 404.

**Actual:** `405 Method Not Allowed`.

**Tests:** `NonExistentBookingTest.writeToDeletedBookingReturns404`

---

<a id="BUG-11"></a>
## BUG-11: Successful cancellation returns 201 Created

**Severity: Low.** This is a convention issue only. Clients that check exactly for `200` or `204` on
delete will report a successful cancellation as unexpected.

**Steps to reproduce**

```bash
ID=$(curl -s -X POST https://restful-booker.herokuapp.com/booking \
  -H 'Content-Type: application/json' -H 'Accept: application/json' \
  -d '{"firstname":"Del","lastname":"Test","totalprice":1,"depositpaid":true,
       "bookingdates":{"checkin":"2031-01-01","checkout":"2031-01-05"}}' \
  | grep -o '"bookingid":[0-9]*' | cut -d: -f2)
curl -i -X DELETE https://restful-booker.herokuapp.com/booking/$ID -u admin:password123
```

**Expected:** `204 No Content` (or `200 OK` with a body).

**Actual:** `201 Created`.

**Tests:** `DeleteBookingTest.deleteReturnsConventionalStatus`

---

### Observations not raised as bugs

- **401 vs 403.** Writes with missing or invalid credentials get `403 Forbidden`. Strictly, missing
  credentials should get `401`, but both deny access, so the tests accept either and focus on the risk
  that matters: the booking is not changed.
- **Same-day and zero-price bookings are accepted.** Day-use rooms and complimentary stays are
  plausible, so this is a product decision, not a defect. It's worth confirming with the product owner.
  `BookingValidationTest.openProductQuestionsArePinned` pins today's behaviour, so a change either way is noticed.
- **No upper bounds.** `totalprice: 1e20` is accepted. Worth a product rule (e.g. a maximum nightly rate)
  to catch typos.
