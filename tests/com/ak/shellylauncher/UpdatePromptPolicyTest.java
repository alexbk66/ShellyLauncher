package com.ak.shellylauncher;

import java.util.ArrayList;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public final class UpdatePromptPolicyTest {
    private static int cases;
    private static final String VERIFIED_MESSAGE = "A new version of Shelly Smart Control is available, "
            + "including important security enhancements, performance optimizations, and stability improvements. "
            + "Update the app from your store to ensure the best and most secure experience.";

    public static void main(String[] args) throws Exception {
        check(5, UpdatePromptPolicy.PACKAGE, dialog(), true, "exact prompt");
        if (args.length != 1) {
            throw new IllegalArgumentException("Supply the sanitized NSPanel fixture path");
        }
        checkVerifiedFixture(args[0]);
        check(-1, "com.eWeLinkControlPanel", dialog(), true, "wrong active package");
        check(-1, UpdatePromptPolicy.PACKAGE, dialog(), false, "truncated or stale snapshot");
        check(-1, UpdatePromptPolicy.PACKAGE, null, true, "missing snapshot");
        List<UpdatePromptPolicy.Element> nodes = dialog();
        nodes.set(3, label("Other title"));
        check(-1, UpdatePromptPolicy.PACKAGE, nodes, true, "no exact title");
        nodes = dialog();
        nodes.set(4, label("Firmware update available"));
        check(-1, UpdatePromptPolicy.PACKAGE, nodes, true, "firmware or generic update dialog");
        nodes = dialog();
        nodes.remove(6);
        check(-1, UpdatePromptPolicy.PACKAGE, nodes, true, "missing store button");
        nodes = dialog();
        nodes.set(5, button("Cancel", true, true, true));
        check(-1, UpdatePromptPolicy.PACKAGE, nodes, true, "never generic cancel");
        nodes = dialog();
        nodes.add(button(UpdatePromptPolicy.DISMISS, true, true, true));
        check(-1, UpdatePromptPolicy.PACKAGE, nodes, true, "ambiguous dismiss buttons");
        nodes = dialog();
        nodes.add(button(UpdatePromptPolicy.STORE, true, true, true));
        check(-1, UpdatePromptPolicy.PACKAGE, nodes, true, "ambiguous store buttons");
        for (int flag = 0; flag < 3; flag++) {
            nodes = dialog();
            nodes.set(5, button(UpdatePromptPolicy.DISMISS, flag != 0, flag != 1, flag != 2));
            check(-1, UpdatePromptPolicy.PACKAGE, nodes, true, "ineligible dismiss button " + flag);
        }
        nodes = dialog();
        nodes.set(5, label(UpdatePromptPolicy.DISMISS));
        check(-1, UpdatePromptPolicy.PACKAGE, nodes, true, "label is not a Button");
        nodes = dialog();
        nodes.set(3, label("Shelly Smart Control aktualisieren"));
        check(-1, UpdatePromptPolicy.PACKAGE, nodes, true, "unrecognized locale");
        nodes = dialog();
        nodes.set(4, label(UpdatePromptPolicy.MESSAGE + " Please confirm."));
        check(-1, UpdatePromptPolicy.PACKAGE, nodes, true, "changed message");
        nodes = dialog();
        nodes.set(4, label(UpdatePromptPolicy.MESSAGE.replace("available. ", "available.\n  ")));
        check(5, UpdatePromptPolicy.PACKAGE, nodes, true, "line wrapping");
        nodes = dialog();
        nodes.set(5, new UpdatePromptPolicy.Element(UpdatePromptPolicy.PACKAGE,
                "android.widget.Button", "", UpdatePromptPolicy.DISMISS, true, true, true, 2));
        check(5, UpdatePromptPolicy.PACKAGE, nodes, true, "accessible description label");
        nodes = dialog();
        nodes.set(5, new UpdatePromptPolicy.Element("other.app", "android.widget.Button",
                UpdatePromptPolicy.DISMISS, "", true, true, true, 2));
        check(-1, UpdatePromptPolicy.PACKAGE, nodes, true, "foreign-package node");
        nodes = dialog();
        nodes.set(3, new UpdatePromptPolicy.Element(UpdatePromptPolicy.PACKAGE,
                "android.view.View", UpdatePromptPolicy.TITLE, "", false, true, false, 2));
        check(-1, UpdatePromptPolicy.PACKAGE, nodes, true, "hidden title");
        nodes = dialog();
        nodes.set(6, button(UpdatePromptPolicy.STORE, true, false, true));
        check(-1, UpdatePromptPolicy.PACKAGE, nodes, true, "disabled store control");
        nodes = dialog();
        nodes.set(5, new UpdatePromptPolicy.Element(UpdatePromptPolicy.PACKAGE,
                "android.widget.Button", "Delete", UpdatePromptPolicy.DISMISS, true, true, true, 2));
        check(-1, UpdatePromptPolicy.PACKAGE, nodes, true, "conflicting accessible labels");
        nodes = dialog();
        nodes.set(5, new UpdatePromptPolicy.Element(UpdatePromptPolicy.PACKAGE,
                "android.widget.Button", UpdatePromptPolicy.DISMISS, "", true, true, true, 1));
        check(-1, UpdatePromptPolicy.PACKAGE, nodes, true, "unrelated dismiss outside prompt container");
        nodes = dialog();
        nodes.set(6, new UpdatePromptPolicy.Element(UpdatePromptPolicy.PACKAGE,
                "android.widget.Button", UpdatePromptPolicy.STORE, "", true, true, true, 1));
        check(-1, UpdatePromptPolicy.PACKAGE, nodes, true, "unrelated store outside prompt container");
        nodes = dialog();
        nodes.set(4, new UpdatePromptPolicy.Element(UpdatePromptPolicy.PACKAGE,
                "android.view.View", UpdatePromptPolicy.MESSAGE, "", true, true, false, 1));
        check(-1, UpdatePromptPolicy.PACKAGE, nodes, true, "title and body only share WebView");
        nodes = dialog();
        nodes.set(2, new UpdatePromptPolicy.Element(UpdatePromptPolicy.PACKAGE,
                "android.view.View", "", "", true, true, false, 2));
        check(-1, UpdatePromptPolicy.PACKAGE, nodes, true, "invalid cyclic parent");
        nodes = dialog();
        nodes.set(4, label(VERIFIED_MESSAGE));
        check(5, UpdatePromptPolicy.PACKAGE, nodes, true, "verified English translation");
        nodes = dialog();
        nodes.set(4, label(VERIFIED_MESSAGE + " Confirm now."));
        check(-1, UpdatePromptPolicy.PACKAGE, nodes, true, "unrecognized translated message");
        System.out.println("PASS: " + cases + " strict update-prompt policy cases");
    }

    private static void checkVerifiedFixture(String path) throws Exception {
        List<UpdatePromptPolicy.Element> nodes = new ArrayList<>();
        int dismissIndex = -1;
        for (String line : Files.readAllLines(Paths.get(path), StandardCharsets.UTF_8)) {
            String[] fields = line.split("\t", -1);
            if (fields.length != 8) {
                throw new AssertionError("Invalid sanitized fixture row");
            }
            if (UpdatePromptPolicy.DISMISS.equals(fields[6])) {
                dismissIndex = nodes.size();
            }
            nodes.add(new UpdatePromptPolicy.Element(fields[1], fields[2], fields[6], fields[7],
                    Boolean.parseBoolean(fields[3]), Boolean.parseBoolean(fields[4]),
                    Boolean.parseBoolean(fields[5]), Integer.parseInt(fields[0])));
        }
        if (dismissIndex < 0) {
            throw new AssertionError("Fixture must contain the verified Not now button");
        }
        check(dismissIndex, UpdatePromptPolicy.PACKAGE, nodes, true,
                "observed NSPanel WebView hierarchy and translated message");
    }

    private static List<UpdatePromptPolicy.Element> dialog() {
        List<UpdatePromptPolicy.Element> nodes = new ArrayList<>();
        nodes.add(container("android.widget.FrameLayout", -1));
        nodes.add(container("android.webkit.WebView", 0));
        nodes.add(container("android.view.View", 1));
        nodes.add(label(UpdatePromptPolicy.TITLE));
        nodes.add(label(UpdatePromptPolicy.MESSAGE));
        nodes.add(button(UpdatePromptPolicy.DISMISS, true, true, true));
        nodes.add(button(UpdatePromptPolicy.STORE, true, true, true));
        return nodes;
    }

    private static UpdatePromptPolicy.Element label(String text) {
        return new UpdatePromptPolicy.Element(UpdatePromptPolicy.PACKAGE,
                "android.view.View", text, "", true, true, false, 2);
    }

    private static UpdatePromptPolicy.Element button(String text, boolean visible,
                                                     boolean enabled, boolean clickable) {
        return new UpdatePromptPolicy.Element(UpdatePromptPolicy.PACKAGE,
                "android.widget.Button", text, "", visible, enabled, clickable, 2);
    }

    private static UpdatePromptPolicy.Element container(String className, int parent) {
        return new UpdatePromptPolicy.Element(UpdatePromptPolicy.PACKAGE,
                className, "", "", true, true, false, parent);
    }

    private static void check(int expected, String packageName,
                              List<UpdatePromptPolicy.Element> nodes,
                              boolean complete, String description) {
        cases++;
        int actual = UpdatePromptPolicy.findDismissButton(packageName, nodes, complete);
        if (actual != expected) {
            throw new AssertionError(description + ": expected " + expected + ", got " + actual);
        }
    }
}
