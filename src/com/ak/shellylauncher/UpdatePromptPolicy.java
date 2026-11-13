package com.ak.shellylauncher;

import java.util.List;

/** Matches only the identified English native-app update modal, never generic updates. */
final class UpdatePromptPolicy {
    static final String PACKAGE = "cloud.shelly.smartcontrol";
    static final String TITLE = "Update Shelly Smart Control";
    static final String MESSAGE = "A newer version of the Shelly Smart Control app is available. "
            + "Update it from the store to continue with the latest version.";
    // Verified against the downloaded English translation and the live NSPanel UI.
    static final String ENGLISH_MESSAGE = "A new version of Shelly Smart Control is available, "
            + "including important security enhancements, performance optimizations, and stability improvements. "
            + "Update the app from your store to ensure the best and most secure experience.";
    static final String DISMISS = "Not now";
    static final String STORE = "Go to Store";

    enum Reason {
        MATCH, INCOMPLETE_SNAPSHOT, WRONG_PACKAGE, MISSING_SNAPSHOT,
        NULL_NODE, FOREIGN_NODE_PACKAGE, INVALID_PARENT,
        TITLE_NOT_UNIQUE, MESSAGE_NOT_UNIQUE, DISMISS_BUTTON_NOT_UNIQUE,
        STORE_BUTTON_NOT_UNIQUE, NO_SHARED_CONTAINER, CONTAINER_IS_ROOT,
        CONTAINER_HIDDEN, CONTAINER_IS_WEBVIEW, NO_WEBVIEW_ANCESTOR,
        DISMISS_OUTSIDE_CONTAINER, STORE_OUTSIDE_CONTAINER
    }

    static final class MatchResult {
        final int buttonIndex;
        final Reason reason;

        MatchResult(int buttonIndex, Reason reason) {
            this.buttonIndex = buttonIndex;
            this.reason = reason;
        }
    }

    static final class Element {
        final String packageName;
        final String className;
        final String text;
        final String description;
        final boolean visible;
        final boolean enabled;
        final boolean clickable;
        final int parentIndex;

        Element(String packageName, String className, String text, String description,
                boolean visible, boolean enabled, boolean clickable, int parentIndex) {
            this.packageName = packageName;
            this.className = className;
            this.text = text;
            this.description = description;
            this.visible = visible;
            this.enabled = enabled;
            this.clickable = clickable;
            this.parentIndex = parentIndex;
        }
    }

    private UpdatePromptPolicy() { }

    static MatchResult evaluate(String activePackage, List<Element> elements, boolean complete) {
        if (!complete) {
            return failure(Reason.INCOMPLETE_SNAPSHOT);
        }
        if (!PACKAGE.equals(activePackage)) {
            return failure(Reason.WRONG_PACKAGE);
        }
        if (elements == null) {
            return failure(Reason.MISSING_SNAPSHOT);
        }
        int titleIndex = -1;
        int messageIndex = -1;
        int titleCount = 0;
        int messageCount = 0;
        int dismissIndex = -1;
        int dismissCount = 0;
        int storeCount = 0;
        for (int i = 0; i < elements.size(); i++) {
            Element element = elements.get(i);
            if (element == null) {
                return failure(Reason.NULL_NODE);
            }
            if (!PACKAGE.equals(element.packageName)) {
                return failure(Reason.FOREIGN_NODE_PACKAGE);
            }
            if (i == 0 ? element.parentIndex != -1
                    : element.parentIndex < 0 || element.parentIndex >= i) {
                return failure(Reason.INVALID_PARENT);
            }
            if (!element.visible) {
                continue;
            }
            if (hasLabel(element, TITLE)) {
                titleIndex = i;
                titleCount++;
            }
            if (hasLabel(element, MESSAGE) || hasLabel(element, ENGLISH_MESSAGE)) {
                messageIndex = i;
                messageCount++;
            }
            if (isButton(element, DISMISS)) {
                dismissIndex = i;
                dismissCount++;
            }
            if (isButton(element, STORE)) {
                storeCount++;
            }
        }
        if (titleCount != 1) {
            return failure(Reason.TITLE_NOT_UNIQUE);
        }
        if (messageCount != 1) {
            return failure(Reason.MESSAGE_NOT_UNIQUE);
        }
        if (dismissCount != 1) {
            return failure(Reason.DISMISS_BUTTON_NOT_UNIQUE);
        }
        if (storeCount != 1) {
            return failure(Reason.STORE_BUTTON_NOT_UNIQUE);
        }
        // The source modal has separate header/content/footer under one container.
        // Require both controls in the smallest container shared by title and body;
        // a window-wide or WebView-wide collection of matching words is insufficient.
        int container = commonAncestor(elements, titleIndex, messageIndex);
        if (container < 0) {
            return failure(Reason.NO_SHARED_CONTAINER);
        }
        if (elements.get(container).parentIndex < 0) {
            return failure(Reason.CONTAINER_IS_ROOT);
        }
        if (!elements.get(container).visible) {
            return failure(Reason.CONTAINER_HIDDEN);
        }
        if ("android.webkit.WebView".equals(elements.get(container).className)) {
            return failure(Reason.CONTAINER_IS_WEBVIEW);
        }
        if (!hasWebViewAncestor(elements, container)) {
            return failure(Reason.NO_WEBVIEW_ANCESTOR);
        }
        if (!isDescendant(elements, dismissIndex, container)) {
            return failure(Reason.DISMISS_OUTSIDE_CONTAINER);
        }
        for (int i = 0; i < elements.size(); i++) {
            if (isButton(elements.get(i), STORE) && !isDescendant(elements, i, container)) {
                return failure(Reason.STORE_OUTSIDE_CONTAINER);
            }
        }
        return new MatchResult(dismissIndex, Reason.MATCH);
    }

    /** Returns the unique Not now node index, or -1 if any required evidence is missing. */
    static int findDismissButton(String activePackage, List<Element> elements, boolean complete) {
        return evaluate(activePackage, elements, complete).buttonIndex;
    }

    private static MatchResult failure(Reason reason) {
        return new MatchResult(-1, reason);
    }

    private static int commonAncestor(List<Element> elements, int first, int second) {
        for (int ancestor = first; ancestor >= 0; ancestor = elements.get(ancestor).parentIndex) {
            if (isDescendant(elements, second, ancestor)) {
                return ancestor;
            }
        }
        return -1;
    }

    private static boolean isDescendant(List<Element> elements, int index, int ancestor) {
        for (int current = index; current >= 0; current = elements.get(current).parentIndex) {
            if (current == ancestor) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasWebViewAncestor(List<Element> elements, int index) {
        for (int current = elements.get(index).parentIndex; current >= 0;
                current = elements.get(current).parentIndex) {
            if ("android.webkit.WebView".equals(elements.get(current).className)) {
                return true;
            }
        }
        return false;
    }

    static boolean isButton(Element element, String label) {
        return element != null && PACKAGE.equals(element.packageName)
                && "android.widget.Button".equals(element.className)
                && element.visible && element.enabled && element.clickable
                && hasLabel(element, label);
    }

    static boolean hasLabel(Element element, String expected) {
        String text = normalize(element.text);
        String description = normalize(element.description);
        if (!text.isEmpty() && !description.isEmpty() && !text.equals(description)) {
            return false;
        }
        return expected.equals(text) || expected.equals(description);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }
}
