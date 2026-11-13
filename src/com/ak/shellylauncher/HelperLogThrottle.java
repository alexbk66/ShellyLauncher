package com.ak.shellylauncher;

/** Fixed-size rate limiter for diagnostic categories; never stores event/UI values. */
final class HelperLogThrottle {
    enum Kind { EVENT, INSPECTION, OUTCOME, RETRY, DETECTION, INTERRUPTION }
    private static final long INTERVAL_MS = 5000;
    private final long[] last = new long[Kind.values().length];
    private final long[] failures = new long[UpdatePromptPolicy.Reason.values().length];

    HelperLogThrottle() {
        java.util.Arrays.fill(last, -1);
        java.util.Arrays.fill(failures, -1);
    }

    boolean shouldLog(Kind kind, long now) {
        return allow(last, kind.ordinal(), now);
    }

    boolean shouldLogFailure(UpdatePromptPolicy.Reason reason, long now) {
        return allow(failures, reason.ordinal(), now);
    }

    private static boolean allow(long[] times, int index, long now) {
        if (now < 0) {
            return false;
        }
        if (times[index] >= 0 && now - times[index] < INTERVAL_MS) {
            return false;
        }
        times[index] = now;
        return true;
    }
}
