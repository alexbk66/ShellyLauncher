# Earlier maintenance proposal — superseded

The user selected a simpler design on 2026-11-13: a single-tap Launcher button
over Shelly, and Android Settings beside Launch Shelly. See
[the current implementation plan](launcher-controls-plan.md). The three-second
gear and separate maintenance screen described below are historical alternatives,
not the implemented design. Installation and new-service enablement still require
approval.

## Recommended entry point

An optional, small visible gear shortcut over the fullscreen application, opened
by holding it for three seconds. A hidden gesture in the HOME activity cannot
receive touch events while Shelly is in front. Notifications/Quick Settings are
not a usable primary entry point on this panel under the reported conditions.

Use a separate user-enabled AccessibilityService solely for an Android-supported
`TYPE_ACCESSIBILITY_OVERLAY`. It would not request window-content retrieval,
gesture injection, key filtering, root, network access or SYSTEM_ALERT_WINDOW.
Its enablement would be independent of the update-prompt service. Nothing is
enabled automatically by installation.

The shortcut would occupy only its own compact density-independent window,
using NOT_FOCUSABLE / NOT_TOUCH_MODAL; other screen areas continue to Shelly.
The shortcut necessarily consumes touches inside its own area, so it must be
visible rather than an invisible gesture strip. Its size/location would use
current display metrics, not fixed screen coordinates or synthetic taps.
Movement beyond touch slop, a second finger, early release or cancellation would
cancel the hold. Opening once would require releasing before another activation.

The maintenance screen would be a separate Activity, with explicit buttons:

- Open Android Settings: `Settings.ACTION_SETTINGS` (verified working by user).
- Return to Shelly: explicit Shelly MainActivity_Default launch on user request.
- Close: finish maintenance and return to the prior foreground application.

Opening maintenance would not invoke the HOME activity, reset the launch guard,
force-stop Shelly, change HOME, change display scaling, or automatically relaunch
Shelly over Settings. Settings must remain open until the user leaves it.

Disable the maintenance accessibility service to remove the shortcut. If a
working physical/on-screen HOME control is later confirmed, a lower-permission
alternative is HOME -> launcher screen -> hold launcher heading -> maintenance;
that option cannot currently guarantee access from the reported fullscreen state.

## Approval and validation still needed

Approve the entry mechanism before implementation. Installation and enabling a
new service/settings remain separate actions requiring explicit approval.
Later device checks must cover display scaling/rotation, touch cancellation,
no accidental activation, Settings staying in front, returning to Shelly,
independent service toggles, and removal on disabling the service.

Android reference:
https://developer.android.com/reference/android/view/WindowManager.LayoutParams
