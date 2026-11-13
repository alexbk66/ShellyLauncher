# Launcher shortcut and Settings button

User's revised design (2026-11-13): a button over Shelly that returns to the
launcher, and Android Settings beside Launch Shelly. This replaces the earlier
three-second gear/maintenance-screen proposal; no additional maintenance screen
is needed. Tap Launcher once; leave Shelly running in the background.

Implement a separate user-enabled accessibility service for the visible
TYPE_ACCESSIBILITY_OVERLAY shortcut. Observe window-state package metadata only;
no window-content retrieval, gestures, root, network, or draw-over-apps permission.
Show the button after a Shelly window event, hide it for other apps, and ignore
events from the shortcut's own window. Until a Shelly event arrives after service
enablement, leave it hidden. Use density-independent sizing and recreate it on
configuration changes. Disable this service to remove the shortcut independently
of the existing popup helper.

Opening the launcher carries an explicit show-launcher intent extra, including
when the Activity/process must be recreated. Suppress and consume that boot's
automatic attempt so it cannot bounce straight back into Shelly. Normal cold HOME
startup still uses the once-per-boot guard. Do not add onResume autolaunch.
Android Settings uses Settings.ACTION_SETTINGS with failure handling.

Steps:
1. Extend guard tests for deliberate launcher entry; test shortcut window-event
   transitions including self-window events and returning to Settings/launcher.
2. Implement shortcut service/configuration and the launcher controls. Version 1.2
   / versionCode 3. Keep popup matching, service, resources and retry policy intact.
3. Run host tests, build/sign/verify API27 APK, inspect both packaged service
   configurations, and request a read-only review.
4. Leave installation, new-service enablement, reboot and commits/pushes pending
   explicit approval. Existing staged changes must remain staged; do not touch
   the Git index.

Later device validation: Settings stays foreground, shortcut appears over Shelly
and disappears on launcher/Settings, deliberately returning HOME does not bounce,
fresh-process entry behaves the same, rotation/scaling keeps the button usable,
and the popup helper still works with the shortcut service enabled.

Local verification: 86 host cases pass (11 launch-guard, 29 popup matching,
14 popup retry, 8 shortcut visibility, 1 diagnostic privacy contract, 9 matching
reasons and 14 log throttling), API27 compilation succeeds, APK v2/v3
signature verification and zip alignment pass. Initial compilation caught the
unavailable View.getAccessibilityWindowId accessor; the implementation now reads
and recycles only the shortcut Button's local accessibility node instead.
Read-only review found no blocking issues. Window-state metadata is not a
foreground-app query: transient system windows can hide the shortcut until a
new Shelly window-state event. Validate those transitions on-device before
claiming always-available shortcut behavior.

Version 1.2 local APK SHA-256:
`e6b32782f2535a21bbf8296ad7a6fd466a39f5917c22fe3934feecf8ae4f34a0`.
The above hash and no-device-operations status describe the initial local build.

## Approved device validation

The user subsequently approved installation, shortcut-service enablement and
testing. The previous accessibility-service setting is backed up in ignored
`build/accessibility-before-shortcut.json`; the popup helper was preserved.

Initial device testing exposed an API27 event issue: the shortcut's own Button
window-state event had window ID -1, while its local node had a valid window ID.
Counters recorded one overlay addition followed by immediate removal. Version
1.2.2 / code 5 preserves an existing overlay for this exact combination of own
package, Button class and unavailable event ID. Launcher Activity events still
hide it. No additional permissions or window-content access were added.

All 90 host cases pass, including four new visibility regressions. API27 build,
signature and alignment verification pass. Read-only review found no blocking
issues. Installed APK SHA-256:
`093fc198ee9bfeac6a4c0e8313b1f99ebad1392a935beaad2d161930583ebf5b`.

Verified on-device: installation succeeds; overlay persists after its own event;
WindowManager reports it on-screen; tapping its current, verified window bounds
increments button_clicks and brings MainActivity forward, removes the overlay,
and leaves the launcher foreground. Later observation shows Android Settings
foreground, overlay absent, both services running. Manual Settings-button
confirmation was requested and the user confirmed Settings opens and stays open.
No Settings values were changed by these tests.

UIAutomator temporarily suppresses accessibility services and disrupted the
attempted automated Settings-button inspection. Those attempts did not validate
the button. Do not use this method to validate overlay persistence or popup
handling. Temporary device hierarchy files were removed; no hierarchy or private
screen contents were retained in project logs or documentation.

No reboot, staging, commit or push was performed during this feature validation.
Rotation/scaling and a new popup with both services enabled remain to be observed.
