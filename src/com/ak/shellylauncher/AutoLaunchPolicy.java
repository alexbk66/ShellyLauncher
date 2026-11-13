package com.ak.shellylauncher;

/** Pure decision logic for the persisted automatic-launch guard. */
final class AutoLaunchPolicy {
    private AutoLaunchPolicy() { }

    static boolean shouldAttempt(boolean attempted, int previousBoot, int currentBoot) {
        return !attempted || (currentBoot >= 0 && currentBoot != previousBoot);
    }

    static boolean shouldAttempt(boolean attempted, int previousBoot, int currentBoot,
                                 boolean deliberatelyShowLauncher) {
        return !deliberatelyShowLauncher && shouldAttempt(attempted, previousBoot, currentBoot);
    }
}
