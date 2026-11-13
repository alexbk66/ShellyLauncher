package com.ak.shellylauncher;

/** Window metadata only; never inspects another application's screen content. */
final class LauncherShortcutPolicy {
    private LauncherShortcutPolicy() { }

    static boolean nextVisibility(boolean currentlyVisible, String packageName,
                                  int eventWindow, int shortcutWindow) {
        return nextVisibility(currentlyVisible, packageName, eventWindow, shortcutWindow, null);
    }

    static boolean nextVisibility(boolean currentlyVisible, String packageName,
                                  int eventWindow, int shortcutWindow, String eventClass) {
        if (shortcutWindow >= 0 && eventWindow == shortcutWindow) {
            return currentlyVisible;
        }
        // API27 strips window IDs for services without window-content access.
        // Accept our overlay's identity (or a child Button), never the launcher Activity.
        if (currentlyVisible && eventWindow < 0
                && "com.ak.shellylauncher".equals(packageName)
                && ("android.widget.Button".equals(eventClass)
                    || "com.ak.shellylauncher.NavigationOverlay".equals(eventClass))) {
            return true;
        }
        return "cloud.shelly.smartcontrol".equals(packageName);
    }
}
