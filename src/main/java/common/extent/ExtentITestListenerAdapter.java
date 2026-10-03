package common.extent;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import common.core.utils.ConfigLoader;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * TestNG listener that writes the Extent Spark report to {@value #REPORT_OUTPUT}.
 * <p>
 * Node hierarchy:
 * <ol>
 *   <li>Test context (the {@code <test name="...">} in the TestNG XML)</li>
 *   <li>Test class</li>
 *   <li>Test method, labelled with its description and tagged with its groups</li>
 *   <li>One child per data-provider row, when the method is parameterised</li>
 * </ol>
 * A failed assertion is reported as a failure (product defect). Any other exception is reported as a failure
 * marked "BROKEN": the test could not reach a verdict (environment or setup problem).
 */
public class ExtentITestListenerAdapter implements ITestListener, ISuiteListener {

    public static final String REPORT_OUTPUT = "test-output/SparkReport/Index.html";

    private static final ConcurrentMap<String, ExtentTest> CONTEXT_NODES = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, ExtentTest> CLASS_NODES = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, ExtentTest> METHOD_NODES = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, ExtentTest> ROW_NODES = new ConcurrentHashMap<>();

    private static ExtentReports extentReports;

    // ─── Suite lifecycle ───

    @Override
    public void onStart(ISuite suite) {
        initReports(suite.getName());
    }

    @Override
    public void onFinish(ISuite suite) {
        flush();
    }

    @Override
    public void onFinish(ITestContext context) {
        flush();
    }

    // ─── Test lifecycle ───

    @Override
    public synchronized void onTestStart(ITestResult result) {
        initReports(result.getTestContext().getSuite().getName());
        String contextName = result.getTestContext().getName();
        String className = result.getMethod().getRealClass().getSimpleName();
        String methodName = result.getMethod().getMethodName();

        ExtentTest contextNode = CONTEXT_NODES.computeIfAbsent(contextName, extentReports::createTest);
        String classKey = contextName + "::" + className;
        ExtentTest classNode = CLASS_NODES.computeIfAbsent(classKey, k -> contextNode.createNode(className));
        ExtentTest methodNode = METHOD_NODES.computeIfAbsent(classKey + "::" + methodName, k -> {
            String description = result.getMethod().getDescription();
            ExtentTest node = classNode.createNode(methodName, description);
            Arrays.stream(result.getMethod().getGroups()).forEach(node::assignCategory);
            return node;
        });

        // A retry (see common.listeners.RetryBrokenTests) logs to the same node as the attempt it replaces.
        Object[] parameters = result.getParameters();
        ExtentTest active = parameters.length == 0
                ? methodNode
                : ROW_NODES.computeIfAbsent(classKey + "::" + methodName + "::" + result.getParameterIndex(),
                        k -> methodNode.createNode(Arrays.toString(parameters)));
        ThreadSafeExtentManager.setExtentTest(active);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        ExtentTest test = ThreadSafeExtentManager.getTest();
        if (test != null) {
            test.pass("Passed");
        }
        ThreadSafeExtentManager.unload();
    }

    @Override
    public void onTestFailure(ITestResult result) {
        ExtentTest test = ThreadSafeExtentManager.getTest();
        Throwable cause = result.getThrowable();
        if (test != null && cause != null) {
            if (!(cause instanceof AssertionError)) {
                test.assignCategory("BROKEN");
                test.warning("BROKEN: the test could not reach a verdict (environment or setup problem), "
                        + "this is not evidence of a product defect.");
            }
            test.fail(cause);
        }
        ThreadSafeExtentManager.unload();
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        ExtentTest test = ThreadSafeExtentManager.getTest();
        if (test != null && result.wasRetried()) {
            test.warning("Attempt broke and is retried (environment or setup problem): " + result.getThrowable());
        } else if (test != null) {
            test.skip(result.getThrowable() != null ? "Skipped: " + result.getThrowable() : "Skipped");
        }
        ThreadSafeExtentManager.unload();
    }

    // ─── Report init/flush ───

    private static synchronized void initReports(String suiteName) {
        if (extentReports != null) {
            return;
        }
        ExtentSparkReporter spark = new ExtentSparkReporter(REPORT_OUTPUT);
        spark.config().setTheme(Theme.STANDARD);
        spark.config().setDocumentTitle(suiteName);
        spark.config().setReportName(suiteName);
        extentReports = new ExtentReports();
        extentReports.attachReporter(spark);
        extentReports.setSystemInfo("Suite", suiteName);
        extentReports.setSystemInfo("Environment", ConfigLoader.getInstance().getEnv());
        extentReports.setSystemInfo("Base URL", ConfigLoader.getInstance().getBaseUrl());
        extentReports.setSystemInfo("Java", System.getProperty("java.version"));
    }

    private static synchronized void flush() {
        if (extentReports != null) {
            extentReports.flush();
        }
    }
}
