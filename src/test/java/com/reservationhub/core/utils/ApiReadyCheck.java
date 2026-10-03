package com.reservationhub.core.utils;

import com.reservationhub.applicationapi.BookingFlowRequests;
import common.core.utils.ConfigLoader;
import common.exception.FrameworkException;
import common.template.HttpReqObject;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import static com.reservationhub.utilities.ReservationHubConstants.PING_HEALTHY_STATUS;

/**
 * Cold-start gate, called once per run from {@link com.reservationhub.core.BaseTest}'s {@code @BeforeSuite}:
 * polls GET /ping until the API answers 201 or the warm-up budget runs out.
 *
 * If the API never comes up, the exception fails the @BeforeSuite method and TestNG skips every test,
 * with one clear message instead of dozens of misleading timeouts.
 */
public final class ApiReadyCheck {

    private static final Duration POLL_INTERVAL = Duration.ofSeconds(3);

    private ApiReadyCheck() {
    }

    public static void ensureApiIsUp() {
        if (!waitUntilHealthy()) {
            throw new FrameworkException("API at " + ConfigLoader.getInstance().getBaseUrl() + " did not become healthy within "
                    + ConfigLoader.getInstance().getWarmupTimeoutSeconds() + "s; aborting instead of reporting false failures.");
        }
    }

    private static boolean waitUntilHealthy() {
        HttpReqObject ping = HttpReqObject.builder().reported(false).build();
        Instant start = Instant.now();
        Instant deadline = start.plusSeconds(ConfigLoader.getInstance().getWarmupTimeoutSeconds());
        int attempt = 0;
        while (Instant.now().isBefore(deadline)) {
            attempt++;
            try {
                int status = BookingFlowRequests.healthCheck(ping).statusCode();
                if (status == PING_HEALTHY_STATUS) {
                    Duration warmup = Duration.between(start, Instant.now());
                    System.out.printf("API healthy after %d attempt(s), %d ms%n", attempt, warmup.toMillis());
                    writeAllureEnvironment(warmup);
                    return true;
                }
                System.out.printf("Warm-up attempt %d: /ping returned %d%n", attempt, status);
            } catch (RuntimeException e) {
                System.out.printf("Warm-up attempt %d: %s%n", attempt, e.getMessage());
            }
            sleep(POLL_INTERVAL);
        }
        return false;
    }

    /** Shown on the Allure overview page, so a reader knows which environment the results came from. */
    private static void writeAllureEnvironment(Duration warmup) {
        Path dir = Path.of(System.getProperty("allure.results.directory", "target/allure-results"));
        String content = String.join(System.lineSeparator(),
                "Env=" + ConfigLoader.getInstance().getEnv(),
                "Base.URL=" + ConfigLoader.getInstance().getBaseUrl(),
                "Java=" + System.getProperty("java.version"),
                "OS=" + System.getProperty("os.name"),
                "Cold.start.warm-up.ms=" + warmup.toMillis());
        try {
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("environment.properties"), content);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
