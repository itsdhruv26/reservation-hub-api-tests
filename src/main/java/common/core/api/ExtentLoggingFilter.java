package common.core.api;

import common.extent.Logger;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

/**
 * Logs each call to the current test's Extent node: method, URL, status and time always; request and
 * response bodies only with verbose.report=true.
 */
public final class ExtentLoggingFilter implements Filter {

    @Override
    public Response filter(FilterableRequestSpecification request,
                           FilterableResponseSpecification responseSpec,
                           FilterContext context) {
        Logger.logInfo("→ " + request.getMethod() + " " + request.getURI());
        Object body = request.getBody();
        if (body != null) {
            Logger.logCodeBlock("Request body:\n" + body);
        }
        long start = System.nanoTime();
        Response response = context.next(request, responseSpec);
        Logger.logInfo("← " + response.statusCode() + " (" + (System.nanoTime() - start) / 1_000_000 + " ms)");
        Logger.logCodeBlock("Response body:\n" + response.asPrettyString());
        return response;
    }
}
