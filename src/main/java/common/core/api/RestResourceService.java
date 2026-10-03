package common.core.api;

import common.template.HttpReqObject;
import io.restassured.http.Method;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;

/**
 * The single place that sends HTTP requests. The applicationapi layer calls it with a route from {@link Routes}
 * and an {@link HttpReqObject}; it never asserts, so callers decide what the response means.
 */
public final class RestResourceService {

    private RestResourceService() {
    }

    public static Response get(String path, HttpReqObject httpReqObject) {
        return execute(Method.GET, path, httpReqObject);
    }

    public static Response post(String path, HttpReqObject httpReqObject) {
        return execute(Method.POST, path, httpReqObject);
    }

    public static Response put(String path, HttpReqObject httpReqObject) {
        return execute(Method.PUT, path, httpReqObject);
    }

    public static Response patch(String path, HttpReqObject httpReqObject) {
        return execute(Method.PATCH, path, httpReqObject);
    }

    public static Response delete(String path, HttpReqObject httpReqObject) {
        return execute(Method.DELETE, path, httpReqObject);
    }

    private static Response execute(Method method, String path, HttpReqObject httpReqObject) {
        RequestSpecification request = given().spec(SpecBuilder.getRequestSpec(httpReqObject));
        if (httpReqObject.getAccept() != null) {
            request.accept(httpReqObject.getAccept());
        }
        if (httpReqObject.getAuth() != null) {
            request = httpReqObject.getAuth().applyTo(request);
        }
        if (httpReqObject.getPathParams() != null) {
            request.pathParams(httpReqObject.getPathParams());
        }
        if (httpReqObject.getQueryParams() != null) {
            request.queryParams(httpReqObject.getQueryParams());
        }
        if (httpReqObject.getRequestBody() != null) {
            request.body(httpReqObject.getRequestBody());
        }
        return request.request(method, path);
    }
}
