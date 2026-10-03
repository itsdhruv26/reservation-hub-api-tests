package common.exception;

/**
 * A setup or configuration problem in the framework itself (missing config key, unreadable test data, a
 * precondition the API would not satisfy). It is not an AssertionError, so reports show the test as broken
 * rather than as a product defect.
 */
public class FrameworkException extends RuntimeException {

    public FrameworkException(String message) {
        super(message);
    }

    public FrameworkException(String message, Throwable cause) {
        super(message, cause);
    }
}
