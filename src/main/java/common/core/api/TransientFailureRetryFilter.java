package common.core.api;

import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

import java.io.IOException;
import java.util.Set;

/**
 * Retries requests that failed for infrastructure reasons (gateway errors, dropped connections)
 * typical of a sleeping Heroku dyno.
 *
 * Deliberately narrow:
 * - 500 is never retried: on this API it is a real defect signal, and retrying would hide it.
 * - POST /booking is never retried: it is not idempotent, so a retry after a lost response
 *   could create a duplicate booking. POST /auth is safe to repeat and is retried.
 */
public final class TransientFailureRetryFilter implements Filter {

    private static final Set<Integer> RETRYABLE_STATUSES = Set.of(502, 503, 504);
    private static final Set<String> IDEMPOTENT_METHODS = Set.of("GET", "HEAD", "OPTIONS", "PUT", "DELETE");

    private final int maxAttempts;
    private final long backoffMs;

    public TransientFailureRetryFilter(int maxAttempts, long backoffMs) {
        this.maxAttempts = maxAttempts;
        this.backoffMs = backoffMs;
    }

    @Override
    public Response filter(FilterableRequestSpecification request,
                           FilterableResponseSpecification response,
                           FilterContext context) {
        boolean retryable = isSafeToRepeat(request);
        for (int attempt = 1; ; attempt++) {
            boolean lastAttempt = !retryable || attempt >= maxAttempts;
            try {
                Response result = context.next(request, response);
                if (lastAttempt || !RETRYABLE_STATUSES.contains(result.statusCode())) {
                    return result;
                }
            } catch (RuntimeException e) {
                if (lastAttempt || !causedByIo(e)) {
                    throw e;
                }
            }
            sleep(backoffMs * attempt);
        }
    }

    private static boolean isSafeToRepeat(FilterableRequestSpecification request) {
        return IDEMPOTENT_METHODS.contains(request.getMethod())
                || ("POST".equals(request.getMethod()) && request.getURI().endsWith("/auth"));
    }

    private static boolean causedByIo(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof IOException) {
                return true;
            }
        }
        return false;
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while backing off before retry", e);
        }
    }
}
