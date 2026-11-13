package com.ak.shellylauncher;

public final class HelperLogThrottleTest {
    public static void main(String[] args) {
        HelperLogThrottle gate = new HelperLogThrottle();
        check(true, gate.shouldLog(HelperLogThrottle.Kind.EVENT, 0), "first event at uptime zero");
        check(false, gate.shouldLog(HelperLogThrottle.Kind.EVENT, 0), "same-tick duplicate");
        check(false, gate.shouldLog(HelperLogThrottle.Kind.EVENT, 4999), "frequent WebView burst");
        check(true, gate.shouldLog(HelperLogThrottle.Kind.EVENT, 5000), "interval boundary");
        check(true, gate.shouldLog(HelperLogThrottle.Kind.INSPECTION, 5000), "independent inspection category");
        check(false, gate.shouldLog(HelperLogThrottle.Kind.EVENT, 9999), "second burst suppressed");
        check(true, gate.shouldLog(HelperLogThrottle.Kind.EVENT, 10000), "next event summary");
        check(false, gate.shouldLog(HelperLogThrottle.Kind.RETRY, -1), "invalid time");
        HelperLogThrottle fresh = new HelperLogThrottle();
        check(true, fresh.shouldLog(HelperLogThrottle.Kind.DETECTION, 1), "fresh service session");
        check(false, fresh.shouldLog(HelperLogThrottle.Kind.DETECTION, 2), "repeat detection throttled");
        check(true, gate.shouldLogFailure(UpdatePromptPolicy.Reason.TITLE_NOT_UNIQUE, 100),
                "first matching failure logged immediately");
        check(false, gate.shouldLogFailure(UpdatePromptPolicy.Reason.TITLE_NOT_UNIQUE, 101),
                "same matching failure throttled");
        check(true, gate.shouldLogFailure(UpdatePromptPolicy.Reason.CONTAINER_IS_WEBVIEW, 101),
                "different matching failure not hidden by earlier reason");
        check(true, gate.shouldLogFailure(UpdatePromptPolicy.Reason.TITLE_NOT_UNIQUE, 5100),
                "matching-failure interval boundary");
        System.out.println("PASS: 14 diagnostic-log throttle cases");
    }

    private static void check(boolean expected, boolean actual, String description) {
        if (expected != actual) {
            throw new AssertionError(description);
        }
    }
}
