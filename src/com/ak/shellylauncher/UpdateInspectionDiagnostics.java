package com.ak.shellylauncher;

import java.util.List;

/** Counters only: no original text, package names or descriptions leave this method. */
final class UpdateInspectionDiagnostics {
    private UpdateInspectionDiagnostics() { }

    static String describe(List<UpdatePromptPolicy.Element> elements) {
        if (elements == null) {
            return "snapshot=none";
        }
        int visible = 0, foreign = 0, title = 0, message = 0, dismiss = 0, store = 0;
        for (UpdatePromptPolicy.Element element : elements) {
            if (element == null) {
                continue;
            }
            if (!UpdatePromptPolicy.PACKAGE.equals(element.packageName)) {
                foreign++;
            }
            if (!element.visible) {
                continue;
            }
            visible++;
            if (UpdatePromptPolicy.hasLabel(element, UpdatePromptPolicy.TITLE)) {
                title++;
            }
            if (UpdatePromptPolicy.hasLabel(element, UpdatePromptPolicy.MESSAGE)
                    || UpdatePromptPolicy.hasLabel(element, UpdatePromptPolicy.ENGLISH_MESSAGE)) {
                message++;
            }
            if (UpdatePromptPolicy.isButton(element, UpdatePromptPolicy.DISMISS)) {
                dismiss++;
            }
            if (UpdatePromptPolicy.isButton(element, UpdatePromptPolicy.STORE)) {
                store++;
            }
        }
        return "nodes=" + elements.size() + " visible=" + visible + " foreign=" + foreign
                + " title=" + title + " message=" + message
                + " dismiss=" + dismiss + " store=" + store;
    }

}
