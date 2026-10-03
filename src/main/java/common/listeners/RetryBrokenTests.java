package common.listeners;

import common.core.utils.ConfigLoader;
import org.testng.IAnnotationTransformer;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;
import org.testng.annotations.ITestAnnotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Gives every test a second chance when it ends "broken": it threw something other than an AssertionError, such
 * as a read timeout from the shared sandbox or a precondition it could not set up. A failed assertion is a
 * finding and is never retried, so a retry cannot hide a product defect.
 * <p>
 * Registered as a listener in the TestNG suite files. The number of retries is {@code brokenTestRetries} in
 * config/{env}.properties.
 */
public final class RetryBrokenTests implements IAnnotationTransformer {

    @Override
    @SuppressWarnings("rawtypes")
    public void transform(ITestAnnotation annotation, Class testClass, Constructor testConstructor, Method testMethod) {
        annotation.setRetryAnalyzer(Analyzer.class);
    }

    /** TestNG creates one instance per test method and data-provider row, so the count is per test. */
    public static final class Analyzer implements IRetryAnalyzer {

        private int retries;

        @Override
        public boolean retry(ITestResult result) {
            Throwable cause = result.getThrowable();
            boolean broken = cause != null && !(cause instanceof AssertionError);
            if (!broken || retries >= ConfigLoader.getInstance().getBrokenTestRetries()) {
                return false;
            }
            retries++;
            System.out.printf("Retrying %s.%s after an infrastructure error (retry %d): %s%n",
                    result.getTestClass().getRealClass().getSimpleName(), result.getMethod().getMethodName(), retries, cause);
            return true;
        }
    }
}
