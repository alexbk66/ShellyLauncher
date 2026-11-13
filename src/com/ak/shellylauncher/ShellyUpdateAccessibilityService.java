package com.ak.shellylauncher;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;
import java.io.FileDescriptor;
import java.io.PrintWriter;

/** Optional, user-enabled helper. Never records, logs or transmits screen contents. */
public final class ShellyUpdateAccessibilityService extends AccessibilityService {
    private static final String TAG = "ShellyUpdateHelper";
    private static final int MAX_NODES = 600;
    private static final int MAX_DEPTH = 24;
    private static final long COOLDOWN_MS = 5000;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final HelperLogThrottle logThrottle = new HelperLogThrottle();
    private long lastClick = -COOLDOWN_MS;
    private boolean inspectionScheduled;
    private long inspectionStartedAt;
    private int retriesScheduled;
    // In-memory counters only. No screen text or user data is retained here.
    private int receivedEvents;
    private int inspections;
    private int clickAttempts;
    private int acceptedClicks;
    private long lastEventUptime;
    private String lastOutcome = "not_inspected";
    private String lastSnapshot = "snapshot=none";
    private UpdatePromptPolicy.Reason lastMatchReason;
    private final Runnable inspectWindow = new Runnable() {
        @Override
        public void run() {
            inspectionScheduled = false;
            if (dismissKnownPrompt()) {
                scheduleUnavailableTreeRetry();
            }
        }
    };

    @Override
    protected void onServiceConnected() {
        // Enabling this service is a separate explicit Android user action.
        AccessibilityServiceInfo info = getServiceInfo();
        Log.i(TAG, "Service connected flags=" + (info == null ? 0 : info.flags));
        scheduleInspection();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event != null && UpdatePromptPolicy.PACKAGE.contentEquals(
                event.getPackageName() == null ? "" : event.getPackageName())) {
            receivedEvents++;
            lastEventUptime = SystemClock.uptimeMillis();
            String type = event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                    ? "WINDOW_STATE_CHANGED" : event.getEventType()
                    == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ? "WINDOW_CONTENT_CHANGED" : "OTHER";
            logThrottled(HelperLogThrottle.Kind.EVENT,
                    "Relevant event=" + type + " received=" + receivedEvents
                    + " inspection_pending=" + inspectionScheduled);
            scheduleInspection();
        }
    }

    private void scheduleInspection() {
        if (inspectionScheduled || SystemClock.uptimeMillis() - lastClick < COOLDOWN_MS) {
            return;
        }
        // Keep the first scheduled inspection: busy WebViews must not postpone it.
        inspectionStartedAt = SystemClock.uptimeMillis();
        retriesScheduled = 0;
        inspectionScheduled = handler.postDelayed(inspectWindow,
                UpdateInspectionRetryPolicy.INITIAL_DELAY_MS);
        if (!inspectionScheduled) {
            logThrottled(HelperLogThrottle.Kind.RETRY, "Initial inspection scheduling rejected");
        }
    }

    private void scheduleUnavailableTreeRetry() {
        long delay = UpdateInspectionRetryPolicy.nextDelayMillis(retriesScheduled,
                SystemClock.uptimeMillis() - inspectionStartedAt);
        if (delay >= 0) {
            retriesScheduled++;
            inspectionScheduled = handler.postDelayed(inspectWindow, delay);
            logThrottled(HelperLogThrottle.Kind.RETRY, "Retry reason=" + lastOutcome
                    + " number=" + retriesScheduled + " delay_ms=" + delay
                    + " scheduled=" + inspectionScheduled);
        } else {
            logThrottled(HelperLogThrottle.Kind.RETRY,
                    "Retry stopped reason=budget_or_deadline last_outcome=" + lastOutcome
                    + " retries=" + retriesScheduled);
        }
    }

    /** Returns true only for a temporarily unavailable/stale tree, never a different UI. */
    private boolean dismissKnownPrompt() {
        inspections++;
        lastSnapshot = "snapshot=none";
        lastMatchReason = null;
        logThrottled(HelperLogThrottle.Kind.INSPECTION,
                "Inspection attempt=" + inspections + " retry=" + retriesScheduled);
        if (SystemClock.uptimeMillis() - lastClick < COOLDOWN_MS) {
            return outcome("cooldown", false);
        }
        List<AccessibilityNodeInfo> nodes = new ArrayList<>();
        List<Integer> depths = new ArrayList<>();
        List<Integer> parents = new ArrayList<>();
        List<UpdatePromptPolicy.Element> elements = new ArrayList<>();
        try {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root == null) {
                return outcome("root_unavailable", true);
            }
            nodes.add(root);
            depths.add(0);
            parents.add(-1);
            String packageName = string(root.getPackageName());
            if (!UpdatePromptPolicy.PACKAGE.equals(packageName)) {
                return outcome("root_other_app", false);
            }
            boolean complete = true;
            String incompleteReason = "incomplete";
            for (int i = 0; i < nodes.size(); i++) {
                AccessibilityNodeInfo node = nodes.get(i);
                elements.add(element(node, parents.get(i)));
                int childCount = node.getChildCount();
                int depth = depths.get(i);
                if ((depth >= MAX_DEPTH && childCount > 0)
                        || childCount > MAX_NODES - nodes.size()) {
                    complete = false;
                    incompleteReason = depth >= MAX_DEPTH ? "depth_limit" : "node_limit";
                    break;
                }
                for (int j = 0; j < childCount; j++) {
                    AccessibilityNodeInfo child = node.getChild(j);
                    if (child == null) {
                        complete = false;
                        incompleteReason = "child_unavailable";
                        break;
                    }
                    nodes.add(child);
                    depths.add(depth + 1);
                    parents.add(i);
                }
                if (!complete) {
                    break;
                }
            }
            lastSnapshot = UpdateInspectionDiagnostics.describe(elements);
            if (!complete) {
                return outcome(incompleteReason, true);
            }
            UpdatePromptPolicy.MatchResult match = UpdatePromptPolicy.evaluate(packageName, elements, complete);
            lastMatchReason = match.reason;
            int index = match.buttonIndex;
            if (index < 0) {
                return outcome("no_matching_prompt", false);
            }
            if (logThrottle.shouldLog(HelperLogThrottle.Kind.DETECTION, SystemClock.uptimeMillis())) {
                Log.i(TAG, "Popup detected: strict snapshot match found");
            }
            AccessibilityNodeInfo button = nodes.get(index);
            if (!button.refresh()) {
                return outcome("button_stale", true);
            }
            if (!UpdatePromptPolicy.isButton(element(button, parents.get(index)),
                    UpdatePromptPolicy.DISMISS)) {
                return outcome("button_changed", false);
            }
            // Do not act on a background window if focus changed while collecting nodes.
            AccessibilityNodeInfo current = getRootInActiveWindow();
            if (current == null) {
                return outcome("focus_unavailable", true);
            }
            try {
                if (current.getWindowId() != root.getWindowId()
                        || !UpdatePromptPolicy.PACKAGE.equals(string(current.getPackageName()))) {
                    return outcome("focus_changed", false);
                }
            } finally {
                current.recycle();
            }
            // Also rate-limit a failed action; never fall back to Back or coordinates.
            lastClick = SystemClock.uptimeMillis();
            clickAttempts++;
            boolean accepted = button.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            // One result per real action; existing five-second cooldown bounds this rate.
            Log.i(TAG, "ACTION_CLICK returned=" + accepted + " attempt=" + clickAttempts);
            if (accepted) {
                acceptedClicks++;
            }
            return outcome(accepted ? "click_accepted" : "click_rejected", false);
        } catch (IllegalStateException unavailable) {
            // A changing/inaccessible tree is not permission to guess. No content logging.
            return outcome("stale_tree", true);
        } catch (SecurityException blocked) {
            // A permission failure is not a transient tree failure.
            return outcome("permission_blocked", false);
        } finally {
            for (AccessibilityNodeInfo node : nodes) {
                node.recycle();
            }
        }
    }

    private boolean outcome(String value, boolean retry) {
        lastOutcome = value;
        String summary = "Inspection outcome=" + value
                + " retry=" + retry + " match_reason="
                + (lastMatchReason == null ? "not_evaluated" : lastMatchReason.name())
                + " " + lastSnapshot;
        if ("no_matching_prompt".equals(value) && lastMatchReason != null) {
            // First occurrence of each fixed reason is visible even during startup;
            // alternating reasons cannot bypass their individual five-second limits.
            if (logThrottle.shouldLogFailure(lastMatchReason, SystemClock.uptimeMillis())) {
                Log.d(TAG, summary);
            }
        } else {
            logThrottled(HelperLogThrottle.Kind.OUTCOME, summary);
        }
        return retry;
    }

    private void logThrottled(HelperLogThrottle.Kind kind, String message) {
        // Callers pass fixed codes, enums and numeric/boolean counters only.
        if (logThrottle.shouldLog(kind, SystemClock.uptimeMillis())) {
            Log.d(TAG, message);
        }
    }

    @Override
    protected void dump(FileDescriptor descriptor, PrintWriter writer, String[] arguments) {
        AccessibilityServiceInfo info = getServiceInfo();
        writer.println("popup_helper_diagnostics=1");
        writer.println("service_flags=" + (info == null ? 0 : info.flags));
        writer.println("events=" + receivedEvents + " inspections=" + inspections);
        writer.println("event_age_ms=" + (lastEventUptime == 0 ? -1
                : SystemClock.uptimeMillis() - lastEventUptime));
        writer.println("inspection_pending=" + inspectionScheduled + " retries=" + retriesScheduled);
        writer.println("last_outcome=" + lastOutcome);
        writer.println("last_match_reason=" + (lastMatchReason == null
                ? "not_evaluated" : lastMatchReason.name()));
        writer.println("last_snapshot=" + lastSnapshot);
        writer.println("click_attempts=" + clickAttempts + " accepted_clicks=" + acceptedClicks);
    }

    private static UpdatePromptPolicy.Element element(AccessibilityNodeInfo node, int parent) {
        return new UpdatePromptPolicy.Element(string(node.getPackageName()),
                string(node.getClassName()), string(node.getText()),
                string(node.getContentDescription()), node.isVisibleToUser(),
                node.isEnabled(), node.isClickable(), parent);
    }

    private static String string(CharSequence value) {
        return value == null ? "" : value.toString();
    }

    @Override
    public void onInterrupt() {
        logThrottled(HelperLogThrottle.Kind.INTERRUPTION, "Service interrupted; pending inspection cancelled");
        handler.removeCallbacks(inspectWindow);
        inspectionScheduled = false;
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Service disconnected: destroyed");
        handler.removeCallbacks(inspectWindow);
        inspectionScheduled = false;
        super.onDestroy();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "Service disconnected: unbound");
        return super.onUnbind(intent);
    }
}
