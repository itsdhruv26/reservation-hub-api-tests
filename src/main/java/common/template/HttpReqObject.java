package common.template;

import common.core.api.Auth;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * Everything a single API call needs besides its method and route. Built by the controllers and handed to
 * the applicationapi layer, which hands it to {@link common.core.api.RestResourceService}.
 *
 * <pre>{@code
 * HttpReqObject req = HttpReqObject.builder().pathParams(Map.of("id", 42)).auth(Auth.token(token)).build();
 * Response response = BookingFlowRequests.deleteBooking(req);
 * }</pre>
 */
@Getter
@Builder
public class HttpReqObject {

    /** A POJO (serialised to JSON) or a raw String body sent verbatim. */
    private Object requestBody;
    /** How the call authenticates; null sends no credentials. */
    private Auth auth;
    /** Values for {placeholders} in the route. Object so tests can also send non-numeric ids. */
    private Map<String, ?> pathParams;
    private Map<String, ?> queryParams;
    /** Replaces the default Accept header (application/json). */
    private String accept;
    /**
     * False for housekeeping traffic (warm-up, cleanup, concurrent calls) that would only add noise to the
     * reports: the call is then neither attached to Allure nor logged to Extent.
     */
    @Builder.Default
    private boolean reported = true;
}
