package com.ak.shellylauncher;

import java.util.ArrayList;
import java.util.List;

public final class UpdateInspectionDiagnosticsTest {
    public static void main(String[] args) {
        if (!"snapshot=none".equals(UpdateInspectionDiagnostics.describe(null))) {
            throw new AssertionError("Null snapshot must be a fixed status code");
        }
        List<UpdatePromptPolicy.Element> nodes = new ArrayList<>();
        String privateValue = "SYNTHETIC_PRIVATE_SENTINEL_7615";
        nodes.add(new UpdatePromptPolicy.Element(privateValue, privateValue,
                privateValue, privateValue, true, true, true, -1));
        String summary = UpdateInspectionDiagnostics.describe(nodes);
        if (summary.contains(privateValue) || !summary.contains("nodes=1")
                || !summary.contains("foreign=1")) {
            throw new AssertionError("Diagnostics must contain counts, never source values");
        }
        nodes.clear();
        nodes.add(new UpdatePromptPolicy.Element(UpdatePromptPolicy.PACKAGE,
                "android.view.View", UpdatePromptPolicy.TITLE, "", true, true, false, -1));
        nodes.add(new UpdatePromptPolicy.Element(UpdatePromptPolicy.PACKAGE,
                "android.view.View", UpdatePromptPolicy.ENGLISH_MESSAGE, "", true, true, false, 0));
        nodes.add(new UpdatePromptPolicy.Element(UpdatePromptPolicy.PACKAGE,
                "android.widget.Button", UpdatePromptPolicy.DISMISS, "", true, true, true, 0));
        nodes.add(new UpdatePromptPolicy.Element(UpdatePromptPolicy.PACKAGE,
                "android.widget.Button", UpdatePromptPolicy.STORE, "", true, true, true, 0));
        summary = UpdateInspectionDiagnostics.describe(nodes);
        if (!summary.contains("title=1") || !summary.contains("message=1")
                || !summary.contains("dismiss=1") || !summary.contains("store=1")
                || summary.contains(UpdatePromptPolicy.TITLE)
                || summary.contains(UpdatePromptPolicy.MESSAGE)
                || summary.contains(UpdatePromptPolicy.PACKAGE)) {
            throw new AssertionError("Matched source strings must also stay out of diagnostics");
        }
        System.out.println("PASS: 1 popup-diagnostics privacy contract");
    }
}
