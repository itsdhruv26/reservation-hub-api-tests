package common.extent;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.markuputils.MarkupHelper;
import common.core.utils.ConfigLoader;

/** Logs to the current thread's Extent test node. A no-op when no test is active on this thread. */
public final class Logger {

    private Logger() {
    }

    public static void logInfo(String info) {
        ExtentTest test = ThreadSafeExtentManager.getTest();
        if (test != null && info != null && !info.isBlank()) {
            test.info(info);
        }
    }

    public static void logSuccess(String message) {
        ExtentTest test = ThreadSafeExtentManager.getTest();
        if (test != null) {
            test.pass(message);
        }
    }

    public static void logFailure(String message) {
        ExtentTest test = ThreadSafeExtentManager.getTest();
        if (test != null) {
            test.fail(message);
        }
    }

    public static void logWarning(String message) {
        ExtentTest test = ThreadSafeExtentManager.getTest();
        if (test != null) {
            test.warning(message);
        }
    }

    /** Request/response bodies: only logged with verbose.report=true, to keep the report readable. */
    public static void logCodeBlock(String content) {
        ExtentTest test = ThreadSafeExtentManager.getTest();
        if (test != null && isVerbose()) {
            test.info(MarkupHelper.createCodeBlock(content));
        }
    }

    public static boolean isVerbose() {
        return ConfigLoader.getInstance().isVerboseReport();
    }
}
