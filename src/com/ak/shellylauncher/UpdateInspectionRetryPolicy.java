package com.ak.shellylauncher;

/** Bounded retry delays only for temporarily unavailable accessibility nodes. */
final class UpdateInspectionRetryPolicy {
    static final long INITIAL_DELAY_MS = 200;
    static final long WINDOW_MS = 8000;
    private static final long[] DELAYS_MS = {300, 600, 1200, 1800, 1800};

    private UpdateInspectionRetryPolicy() { }

    static long nextDelayMillis(int retriesAlreadyScheduled, long elapsedMillis) {
        if (retriesAlreadyScheduled < 0 || retriesAlreadyScheduled >= DELAYS_MS.length
                || elapsedMillis < 0 || elapsedMillis >= WINDOW_MS) {
            return -1;
        }
        long delay = DELAYS_MS[retriesAlreadyScheduled];
        return delay <= WINDOW_MS - elapsedMillis ? delay : -1;
    }
}
