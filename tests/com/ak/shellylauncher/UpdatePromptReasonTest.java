package com.ak.shellylauncher;

import java.util.ArrayList;
import java.util.List;

public final class UpdatePromptReasonTest {
    private static int cases;

    public static void main(String[] args) {
        expect(UpdatePromptPolicy.Reason.MATCH, UpdatePromptPolicy.PACKAGE, dialog(), true);
        expect(UpdatePromptPolicy.Reason.WRONG_PACKAGE, "other.app", dialog(), true);
        expect(UpdatePromptPolicy.Reason.INCOMPLETE_SNAPSHOT, UpdatePromptPolicy.PACKAGE, dialog(), false);
        expect(UpdatePromptPolicy.Reason.MISSING_SNAPSHOT, UpdatePromptPolicy.PACKAGE, null, true);
        List<UpdatePromptPolicy.Element> nodes = dialog();
        nodes.set(3, element("android.view.View", "Other title", "", false, 2));
        expect(UpdatePromptPolicy.Reason.TITLE_NOT_UNIQUE, UpdatePromptPolicy.PACKAGE, nodes, true);
        nodes = dialog();
        nodes.set(5, element("android.widget.Button", UpdatePromptPolicy.DISMISS, "", true, 1));
        expect(UpdatePromptPolicy.Reason.DISMISS_OUTSIDE_CONTAINER, UpdatePromptPolicy.PACKAGE, nodes, true);
        nodes = dialog();
        nodes.set(3, element("android.view.View", UpdatePromptPolicy.TITLE,
                "SYNTHETIC_PRIVATE_DESCRIPTION", false, 2));
        expect(UpdatePromptPolicy.Reason.TITLE_NOT_UNIQUE, UpdatePromptPolicy.PACKAGE, nodes, true);
        nodes = dialog();
        nodes.set(1, element("android.view.View", "", "", false, 0));
        expect(UpdatePromptPolicy.Reason.NO_WEBVIEW_ANCESTOR, UpdatePromptPolicy.PACKAGE, nodes, true);
        nodes = dialog();
        nodes.set(4, element("android.view.View", UpdatePromptPolicy.MESSAGE, "", false, 1));
        expect(UpdatePromptPolicy.Reason.CONTAINER_IS_WEBVIEW, UpdatePromptPolicy.PACKAGE, nodes, true);
        System.out.println("PASS: " + cases + " exact popup-failure reason cases");
    }

    private static List<UpdatePromptPolicy.Element> dialog() {
        List<UpdatePromptPolicy.Element> nodes = new ArrayList<>();
        nodes.add(element("android.widget.FrameLayout", "", "", false, -1));
        nodes.add(element("android.webkit.WebView", "", "", false, 0));
        nodes.add(element("android.view.View", "", "", false, 1));
        nodes.add(element("android.view.View", UpdatePromptPolicy.TITLE, "", false, 2));
        nodes.add(element("android.view.View", UpdatePromptPolicy.MESSAGE, "", false, 2));
        nodes.add(element("android.widget.Button", UpdatePromptPolicy.DISMISS, "", true, 2));
        nodes.add(element("android.widget.Button", UpdatePromptPolicy.STORE, "", true, 2));
        return nodes;
    }

    private static UpdatePromptPolicy.Element element(String className, String text,
                                                       String description, boolean clickable, int parent) {
        return new UpdatePromptPolicy.Element(UpdatePromptPolicy.PACKAGE, className,
                text, description, true, true, clickable, parent);
    }

    private static void expect(UpdatePromptPolicy.Reason reason, String packageName,
                                List<UpdatePromptPolicy.Element> nodes, boolean complete) {
        cases++;
        UpdatePromptPolicy.MatchResult result = UpdatePromptPolicy.evaluate(packageName, nodes, complete);
        int index = UpdatePromptPolicy.findDismissButton(packageName, nodes, complete);
        if (result.reason != reason || result.buttonIndex != index
                || (reason == UpdatePromptPolicy.Reason.MATCH ? index != 5 : index != -1)) {
            throw new AssertionError("Expected fixed reason " + reason + ", got " + result.reason);
        }
    }
}
