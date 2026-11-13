package com.ak.shellylauncher;

public final class LauncherShortcutPolicyTest {
    public static void main(String[] args) {
        boolean visible = next(false, "cloud.shelly.smartcontrol", 10, -1);
        check(true, visible, "Shelly window shows shortcut");
        visible = next(visible, "com.ak.shellylauncher", 11, 11);
        check(true, visible, "shortcut's own window event must not hide it");
        visible = next(visible, "com.ak.shellylauncher", 12, 11);
        check(false, visible, "opening launcher hides shortcut");
        visible = next(visible, "com.android.settings", 13, -1);
        check(false, visible, "Settings remains unobstructed");
        visible = next(visible, "cloud.shelly.smartcontrol", 10, -1);
        check(true, visible, "returning to Shelly shows shortcut again");
        check(false, next(visible, null, 14, 11), "unknown package hides shortcut");
        check(false, next(visible, "other.app", -1, -1), "invalid IDs are not self-window events");
        check(false, next(false, "cloud.shelly.smartcontrol.extra", 10, -1), "exact package only");
        check(true, LauncherShortcutPolicy.nextVisibility(true, "com.ak.shellylauncher", -1, 39,
                "android.widget.Button"), "API27 overlay event without window ID preserves shortcut");
        check(false, LauncherShortcutPolicy.nextVisibility(true, "com.ak.shellylauncher", -1, 39,
                "com.ak.shellylauncher.MainActivity"), "launcher without window ID hides shortcut");
        check(false, LauncherShortcutPolicy.nextVisibility(false, "com.ak.shellylauncher", -1, -1,
                "android.widget.Button"), "self event cannot create shortcut");
        check(false, LauncherShortcutPolicy.nextVisibility(true, "other.app", -1, 39,
                "android.widget.Button"), "another app button cannot preserve shortcut");
        check(true, LauncherShortcutPolicy.nextVisibility(true, "com.ak.shellylauncher", -1, 39,
                "com.ak.shellylauncher.NavigationOverlay"), "composite navigation overlay preserves itself");
        check(false, LauncherShortcutPolicy.nextVisibility(false, "com.ak.shellylauncher", -1, -1,
                "com.ak.shellylauncher.NavigationOverlay"), "composite self event cannot create overlay");
        System.out.println("PASS: 14 launcher-shortcut visibility cases");
    }

    private static boolean next(boolean visible, String packageName, int event, int overlay) {
        return LauncherShortcutPolicy.nextVisibility(visible, packageName, event, overlay);
    }

    private static void check(boolean expected, boolean actual, String description) {
        if (expected != actual) {
            throw new AssertionError(description);
        }
    }
}
