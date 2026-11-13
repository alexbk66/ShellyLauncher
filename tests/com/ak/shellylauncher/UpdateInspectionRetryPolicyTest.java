package com.ak.shellylauncher;

public final class UpdateInspectionRetryPolicyTest {
    public static void main(String[] args) {
        check(300, 0, 200, "first temporarily unavailable tree");
        check(600, 1, 500, "second unavailable tree");
        check(1200, 2, 1100, "third unavailable tree");
        check(1800, 3, 2300, "fourth unavailable tree");
        check(1800, 4, 4100, "last allowed retry");
        check(-1, 5, 5900, "retry budget exhausted");
        check(-1, 0, 8000, "window expired");
        check(-1, 0, 7900, "retry would cross deadline");
        check(300, 0, 7700, "retry at exact deadline");
        check(-1, 4, 7000, "late callback must not extend window");
        check(-1, -1, 0, "invalid retry count");
        check(-1, 0, -1, "invalid elapsed time");
        check(-1, Integer.MAX_VALUE, 0, "large retry count");
        check(-1, 0, Long.MAX_VALUE, "large elapsed time");
        System.out.println("PASS: 14 bounded accessibility-retry policy cases");
    }

    private static void check(long expected, int retries, long elapsed, String description) {
        long actual = UpdateInspectionRetryPolicy.nextDelayMillis(retries, elapsed);
        if (actual != expected) {
            throw new AssertionError(description + ": expected " + expected + ", got " + actual);
        }
    }
}
