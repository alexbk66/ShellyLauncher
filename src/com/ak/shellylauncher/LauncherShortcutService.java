package com.ak.shellylauncher;

import android.accessibilityservice.AccessibilityService;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import java.io.FileDescriptor;
import java.io.PrintWriter;

/** Independent, opt-in launcher shortcut. Observes window metadata, not screen contents. */
public final class LauncherShortcutService extends AccessibilityService {
    private WindowManager windowManager;
    private LinearLayout shortcut;
    private LinearLayout actions;
    private WindowManager.LayoutParams overlayParameters;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable collapse = new Runnable() {
        @Override public void run() { setExpanded(false); }
    };
    private boolean expanded;
    private int anchorX;
    private int anchorY;
    private int backActions;
    private int recentActions;
    private int drags;
    private int events;
    private int addAttempts;
    private int removedWindows;
    private int buttonClicks;
    private int lastEventWindow = -1;
    private int lastShortcutWindow = -1;
    private boolean eventFromShelly;
    private boolean eventFromOwnApp;
    private boolean eventFromButton;
    private boolean desiredVisible;
    private String lastOperation = "not_connected";
    private String lastOwnEventClass = "none";
    private final String[] transitions = new String[8];

    @Override
    protected void onServiceConnected() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        lastOperation = "connected_waiting_for_shelly_event";
        // Wait for a Shelly window event; do not guess which app is foreground.
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return;
        }
        CharSequence packageName = event.getPackageName();
        events++;
        lastEventWindow = event.getWindowId();
        lastShortcutWindow = shortcutWindowId();
        eventFromShelly = "cloud.shelly.smartcontrol".contentEquals(packageName == null ? "" : packageName);
        eventFromOwnApp = getPackageName().contentEquals(packageName == null ? "" : packageName);
        if (eventFromOwnApp) {
            lastOwnEventClass = event.getClassName() == null ? "none" : event.getClassName().toString();
        }
        eventFromButton = "android.widget.Button".contentEquals(
                event.getClassName() == null ? "" : event.getClassName());
        boolean show = LauncherShortcutPolicy.nextVisibility(shortcut != null,
                packageName == null ? null : packageName.toString(), lastEventWindow,
                lastShortcutWindow, event.getClassName() == null ? null : event.getClassName().toString());
        desiredVisible = show;
        transitions[(events - 1) % transitions.length] = "event=" + events
                + " from=" + (eventFromOwnApp ? "own:" + lastOwnEventClass
                    : eventFromShelly ? "shelly" : "other")
                + " present=" + (shortcut != null) + " keep=" + show
                + " event_window=" + lastEventWindow + " overlay_window=" + lastShortcutWindow;
        if (show) {
            showShortcut();
        } else {
            removeShortcut();
        }
    }

    private void showShortcut() {
        if (shortcut != null || windowManager == null) {
            return;
        }
        LinearLayout panel = new LinearLayout(this) {
            @Override public CharSequence getAccessibilityClassName() {
                return "com.ak.shellylauncher.NavigationOverlay";
            }
        };
        panel.setOrientation(LinearLayout.HORIZONTAL);
        GradientDrawable background = new GradientDrawable();
        background.setColor(0xE622262A);
        background.setCornerRadius(dp(24));
        panel.setBackground(background);
        Button handle = icon("\u2630", "Navigation controls; drag to move", new View.OnClickListener() {
            @Override public void onClick(View view) { setExpanded(!expanded); }
        });
        panel.addView(handle, new LinearLayout.LayoutParams(dp(48), dp(48)));
        actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.addView(icon("\u25C1", "Back", new View.OnClickListener() {
            @Override public void onClick(View view) {
                backActions++;
                navigate(GLOBAL_ACTION_BACK);
            }
        }), new LinearLayout.LayoutParams(dp(48), dp(48)));
        actions.addView(icon("\u2302", "Shelly Launcher", new View.OnClickListener() {
            @Override public void onClick(View view) { openLauncher(); }
        }), new LinearLayout.LayoutParams(dp(48), dp(48)));
        actions.addView(icon("\u25A1", "Recent apps", new View.OnClickListener() {
            @Override public void onClick(View view) {
                recentActions++;
                navigate(GLOBAL_ACTION_RECENTS);
            }
        }), new LinearLayout.LayoutParams(dp(48), dp(48)));
        actions.setVisibility(View.GONE);
        panel.addView(actions);
        expanded = false;
        WindowManager.LayoutParams parameters = new WindowManager.LayoutParams(
                dp(48), dp(48),
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        parameters.gravity = Gravity.TOP | Gravity.LEFT;
        Point screen = screenSize();
        parameters.x = Math.round(getSharedPreferences("navigation", MODE_PRIVATE)
                .getFloat("x", 0.6f) * Math.max(0, screen.x - parameters.width));
        parameters.y = Math.round(getSharedPreferences("navigation", MODE_PRIVATE)
                .getFloat("y", 0.025f) * Math.max(0, screen.y - parameters.height));
        anchorX = parameters.x;
        anchorY = parameters.y;
        overlayParameters = parameters;
        enableDrag(handle);
        shortcut = panel;
        addAttempts++;
        try {
            windowManager.addView(panel, parameters);
            lastOperation = "overlay_added";
        } catch (WindowManager.BadTokenException unavailable) {
            lastOperation = "bad_window_token";
            shortcut = null;
        } catch (IllegalStateException unavailable) {
            lastOperation = "invalid_window_state";
            shortcut = null;
        } catch (SecurityException unavailable) {
            lastOperation = "window_permission_blocked";
            shortcut = null;
        }
    }

    private Button icon(String symbol, String description, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(symbol);
        button.setTextSize(24);
        button.setTextColor(Color.WHITE);
        button.setContentDescription(description);
        button.setAllCaps(false);
        button.setPadding(0, 0, 0, 0);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setOnClickListener(listener);
        return button;
    }

    private Point screenSize() {
        Point size = new Point();
        windowManager.getDefaultDisplay().getSize(size);
        return size;
    }

    private void updatePosition() {
        if (shortcut == null) { return; }
        Point screen = screenSize();
        overlayParameters.x = Math.max(0, Math.min(anchorX,
                Math.max(0, screen.x - overlayParameters.width)));
        overlayParameters.y = Math.max(0, Math.min(anchorY,
                Math.max(0, screen.y - overlayParameters.height)));
        try {
            windowManager.updateViewLayout(shortcut, overlayParameters);
        } catch (IllegalArgumentException unavailable) {
            removeShortcut();
            lastOperation = "overlay_update_failed";
        }
    }

    private void setExpanded(boolean show) {
        handler.removeCallbacks(collapse);
        if (shortcut == null || actions == null) { return; }
        expanded = show;
        actions.setVisibility(show ? View.VISIBLE : View.GONE);
        overlayParameters.width = dp(show ? 192 : 48);
        updatePosition();
        if (show) { handler.postDelayed(collapse, 5000); }
    }

    private void navigate(int action) {
        setExpanded(false);
        boolean accepted = performGlobalAction(action);
        lastOperation = accepted ? "navigation_action_accepted" : "navigation_action_rejected";
        if (!accepted) {
            Toast.makeText(this, "Navigation action unavailable.", Toast.LENGTH_SHORT).show();
        }
    }

    private void savePosition() {
        Point screen = screenSize();
        getSharedPreferences("navigation", MODE_PRIVATE).edit()
                .putFloat("x", (float) overlayParameters.x / Math.max(1, screen.x - overlayParameters.width))
                .putFloat("y", (float) overlayParameters.y / Math.max(1, screen.y - overlayParameters.height))
                .apply();
    }

    private void enableDrag(final Button handle) {
        handle.setOnTouchListener(new View.OnTouchListener() {
            private float downX, downY;
            private int startX, startY;
            private boolean dragging;
            private boolean cancelled;
            @Override public boolean onTouch(View view, MotionEvent event) {
                if (shortcut == null) { return false; }
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        cancelled = false;
                        handler.removeCallbacks(collapse);
                        downX = event.getRawX(); downY = event.getRawY();
                        startX = overlayParameters.x; startY = overlayParameters.y;
                        dragging = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        if (cancelled) { return true; }
                        float dx = event.getRawX() - downX, dy = event.getRawY() - downY;
                        int slop = ViewConfiguration.get(LauncherShortcutService.this).getScaledTouchSlop();
                        if (dx * dx + dy * dy > slop * slop) { dragging = true; }
                        if (dragging) {
                            Point screen = screenSize();
                            anchorX = Math.max(0, Math.min(startX + Math.round(dx), screen.x - dp(48)));
                            anchorY = Math.max(0, Math.min(startY + Math.round(dy), screen.y - dp(48)));
                            updatePosition();
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (cancelled) { return true; }
                        if (dragging) {
                            drags++;
                            setExpanded(false);
                            savePosition();
                        } else { handle.performClick(); }
                        return true;
                    case MotionEvent.ACTION_POINTER_DOWN:
                    case MotionEvent.ACTION_CANCEL:
                        cancelled = true;
                        dragging = false;
                        setExpanded(false);
                        return true;
                    default: return true;
                }
            }
        });
    }

    private void openLauncher() {
        buttonClicks++;
        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(MainActivity.EXTRA_SHOW_LAUNCHER, true);
        removeShortcut();
        try {
            // Bring HOME forward without finishing, stopping or modifying Shelly.
            startActivity(intent);
            lastOperation = "launcher_requested";
        } catch (ActivityNotFoundException | SecurityException unavailable) {
            showShortcut();
            Toast.makeText(this, "Unable to open Shelly Launcher.", Toast.LENGTH_SHORT).show();
            lastOperation = "launcher_request_failed";
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private int shortcutWindowId() {
        if (shortcut == null) {
            return -1;
        }
        // Read only our own overlay's local node; View's direct accessor is not API27.
        AccessibilityNodeInfo node = shortcut.createAccessibilityNodeInfo();
        if (node == null) {
            return -1;
        }
        try {
            return node.getWindowId();
        } finally {
            node.recycle();
        }
    }

    private void removeShortcut() {
        handler.removeCallbacks(collapse);
        expanded = false;
        if (shortcut != null) {
            try {
                windowManager.removeView(shortcut);
            } catch (IllegalArgumentException alreadyRemoved) {
                // The system may already have detached the window.
            } finally {
                removedWindows++;
                lastOperation = "overlay_removed";
                shortcut = null;
                actions = null;
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        boolean wasVisible = shortcut != null;
        removeShortcut();
        if (wasVisible) {
            showShortcut();
        }
    }

    @Override
    public void onInterrupt() {
        removeShortcut();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        removeShortcut();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        removeShortcut();
        super.onDestroy();
    }

    @Override
    protected void dump(FileDescriptor descriptor, PrintWriter writer, String[] arguments) {
        writer.println("launcher_shortcut_diagnostics=1");
        writer.println("window_manager_ready=" + (windowManager != null)
                + " shortcut_present=" + (shortcut != null));
        writer.println("events=" + events + " add_attempts=" + addAttempts
                + " removed_windows=" + removedWindows + " button_clicks=" + buttonClicks);
        writer.println("last_event_window=" + lastEventWindow + " last_shortcut_window=" + lastShortcutWindow);
        writer.println("event_shelly=" + eventFromShelly + " event_own_app=" + eventFromOwnApp
                + " event_button=" + eventFromButton + " desired_visible=" + desiredVisible);
        writer.println("last_operation=" + lastOperation);
        writer.println("expanded=" + expanded + " back_actions=" + backActions
                + " recent_actions=" + recentActions + " drags=" + drags);
        // Class metadata from our own app only; never event text or another app's class.
        writer.println("last_own_event_class=" + lastOwnEventClass);
        for (int sequence = Math.max(0, events - transitions.length); sequence < events; sequence++) {
            writer.println(transitions[sequence % transitions.length]);
        }
    }
}
