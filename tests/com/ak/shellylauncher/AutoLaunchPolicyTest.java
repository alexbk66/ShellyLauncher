package com.ak.shellylauncher;

public final class AutoLaunchPolicyTest {
    public static void main(String[] args) {
        check(true, false, -1, 10, "first start");
        check(false, true, 10, 10, "return HOME or recreate process");
        check(true, true, 10, 11, "new boot");
        check(true, false, -1, -1, "first start without boot counter");
        check(false, true, -1, -1, "unknown boot counter must not loop");
        check(false, true, 10, -1, "unreadable counter must not retry");
        check(true, true, -1, 11, "counter becomes available");
        System.out.println("PASS: 7 automatic-launch policy cases");
    }

    private static void check(boolean expected, boolean attempted, int previous,
                              int current, String description) {
        if (AutoLaunchPolicy.shouldAttempt(attempted, previous, current) != expected) {
            throw new AssertionError(description);
        }
    }
}
