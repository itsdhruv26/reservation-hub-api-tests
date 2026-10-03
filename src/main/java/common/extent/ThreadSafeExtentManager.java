package common.extent;

import com.aventstack.extentreports.ExtentTest;

/**
 * Holds the current thread's ExtentTest node. The listener sets it when a test starts; {@link Logger} and
 * {@link common.core.utils.Assertions} write to it. Threads without a node (e.g. a worker pool inside a
 * test) simply log nothing.
 */
public final class ThreadSafeExtentManager {

    private static final ThreadLocal<ExtentTest> EXTENT_TEST = new ThreadLocal<>();

    private ThreadSafeExtentManager() {
    }

    public static void setExtentTest(ExtentTest test) {
        EXTENT_TEST.set(test);
    }

    public static ExtentTest getTest() {
        return EXTENT_TEST.get();
    }

    public static void unload() {
        EXTENT_TEST.remove();
    }
}
