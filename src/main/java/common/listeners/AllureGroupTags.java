package common.listeners;

import io.qameta.allure.Allure;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;

import java.util.Arrays;

/**
 * Shows each test's TestNG groups (smoke, known-defect, perf) as tags in the Allure report, so a reader can filter
 * by them. Runs just before the test method, when Allure's result for the test already exists.
 */
public final class AllureGroupTags implements IInvokedMethodListener {

    @Override
    public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
        if (method.isTestMethod()) {
            Arrays.stream(method.getTestMethod().getGroups()).forEach(group -> Allure.label("tag", group));
        }
    }
}
