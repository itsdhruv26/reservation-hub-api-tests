package com.reservationhub.tests;

import com.reservationhub.builder.BookingRequestBuilder;
import com.reservationhub.http.RequestSpecs;
import com.reservationhub.config.Config;
import com.reservationhub.controller.BookingController;
import com.reservationhub.model.Booking;
import com.reservationhub.model.CreatedBooking;
import com.reservationhub.support.BaseApiTest;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.reservationhub.support.Groups.PERF;
import static org.assertj.core.api.Assertions.assertThat;

@Epic("Bookings API")
@Feature("Performance smoke")
public class PerformanceSmokeTest extends BaseApiTest {

    /** Outcome of one concurrent create. */
    record Result(Booking sent, int status, Integer bookingId, Booking stored, long latencyMs) {
    }

    @Test(description = "Concurrent creates all succeed, get distinct ids and keep their own data", groups = PERF)
    @Severity(SeverityLevel.NORMAL)
    @Description("Not a load test: a quick check that a burst of parallel partner requests doesn't fail, "
            + "collide on ids, or mix up data, plus an indicative latency figure.")
    public void concurrentCreates() {
        int count = Config.perfConcurrentCreates();
        // Unreported client: Allure's per-request attachments are not thread-safe; a summary is attached instead.
        BookingController client = new BookingController(RequestSpecs.unreported());
        ExecutorService pool = Executors.newFixedThreadPool(count);
        List<Result> results;
        try {
            List<CompletableFuture<Result>> futures = IntStream.range(0, count)
                    .mapToObj(i -> CompletableFuture.supplyAsync(() -> timedCreate(client), pool))
                    .toList();
            results = futures.stream().map(CompletableFuture::join).toList();
        } finally {
            pool.shutdown();
        }
        results.stream().filter(r -> r.bookingId() != null).forEach(r -> cleanup.track(r.bookingId()));
        Allure.addAttachment("Latency summary", "text/plain", summarise(results));

        assertThat(results).as("Every concurrent create should succeed").allMatch(r -> r.status() == 200);
        assertThat(results.stream().map(Result::bookingId).distinct().count())
                .as("Every booking should get its own id").isEqualTo((long) count);
        assertThat(results).as("Each response should carry the booking that was sent, not another request's data")
                .allMatch(r -> r.sent().equals(r.stored()));
        assertThat(p95(results))
                .as("p95 latency (ms) for a create on the shared sandbox")
                .isLessThan(Config.perfP95ThresholdMs());
    }

    private static Result timedCreate(BookingController client) {
        Booking booking = BookingRequestBuilder.aBooking().build();
        long start = System.nanoTime();
        Response response = client.create(booking);
        long latencyMs = (System.nanoTime() - start) / 1_000_000;
        CreatedBooking created = response.statusCode() == 200 ? response.as(CreatedBooking.class) : null;
        return new Result(booking, response.statusCode(),
                created != null ? created.getBookingid() : null,
                created != null ? created.getBooking() : null,
                latencyMs);
    }

    private static long p95(List<Result> results) {
        List<Long> sorted = results.stream().map(Result::latencyMs).sorted().toList();
        return sorted.get((int) Math.ceil(0.95 * sorted.size()) - 1);
    }

    private static String summarise(List<Result> results) {
        List<Result> byLatency = new ArrayList<>(results);
        byLatency.sort(Comparator.comparingLong(Result::latencyMs));
        return "requests: " + results.size()
                + "\nmin ms: " + byLatency.get(0).latencyMs()
                + "\np95 ms: " + p95(results)
                + "\nmax ms: " + byLatency.get(byLatency.size() - 1).latencyMs()
                + "\nstatuses: " + results.stream().collect(Collectors.groupingBy(Result::status, Collectors.counting()));
    }
}
