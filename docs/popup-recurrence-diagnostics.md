# Popup recurrence investigation — 2026-11-13

The user reported the update popup again after initial helper activation.
Read-only checks confirmed the panel still runs launcher 1.1 / versionCode 2,
accessibility is enabled, the popup service is bound, and its capability is 1
(window-content retrieval). The exact known popup was visible in UIAutomator.
Version 1.2 and the Launcher shortcut service have not been installed/enabled.

The earlier before/after disappearance did not establish reliable dismissal.
The failure cause is not yet proven: likely branches include tree unavailable,
traversal limits, missing/flattened context, stale nodes, missed events or a
rejected click. Do not loosen matching or declare one of these confirmed without
the service's own evidence.

## Measurement limitation

Android's UiAutomation suppresses other accessibility services by default.
UIAutomator's default uncompressed dump also requests a different hierarchy
view from a service that does not include non-important views. Therefore a
successful UIAutomator dump cannot establish what this live service saw or did.
A compressed read during this investigation returned a popup-free tree; that
alone does not establish why the popup disappeared. No click was injected.

Primary references:
- https://android.googlesource.com/platform/frameworks/base/+/android-8.1.0_r27/core/java/android/app/UiAutomation.java
- https://android.googlesource.com/platform/frameworks/uiautomator/+/dec12c69b4e09679f691050d8fdcab0a5e474258/cmds/uiautomator/src/com/android/commands/uiautomator/DumpCommand.java
- https://developer.android.com/reference/android/accessibilityservice/AccessibilityServiceInfo

## Separate diagnostic artifact

Prepared `dist/ShellyLauncher-popup-diagnostic.apk`, version 1.1.1 / code 3,
SHA-256 `aaea1e793e5b99abf88354bb1718e1cf8bd63c557e6a2eba3c139164799094ca`.
It is exported from the staged popup-only source, with diagnostic instrumentation
overlaid. The user index is unchanged. The original signing key was referenced
in place; it was not copied. No LauncherShortcutService, maintenance UI, new
permission or new accessibility capability is included. Matching, retry timing,
cooldown and click decisions are unchanged.

The refreshed logging build passed 74 host checks (7 guard, 29 prompt, 14 retry,
1 diagnostic privacy contract, 9 matching-reason cases, 14 throttle cases),
API27 compilation, v2/v3 signature verification and zip alignment.
Packaged XML confirms one popup service and the original configuration.
Read-only code review found no blocking privacy or behavior-preservation issue.

The service's dump contains only fixed outcome codes, counts, flags, event age,
pending/retry state, and ACTION_CLICK attempt/acceptance counts. No original
screen text, descriptions, package values, exception messages or credentials
are retained in diagnostic fields or emitted. Counters are in-memory only.
Counts now use the same strict label predicates as the policy, including
conflicting-label rejection. Positive counts still do not prove shared context
or stable focus. Accepted clicks record Android's return, not visual confirmation.

The user's subsequent Logcat request added throttled Android logging under
`ShellyUpdateHelper` and exact fixed policy-failure reasons. See
`docs/update-helper-logging.md`. The policy now returns a typed reason and index
from the same decision path; all prior acceptance/rejection conditions remain.

After explicit installation approval, read diagnostics without UIAutomator:

```text
adb -s 192.168.68.124:5555 shell dumpsys activity service com.ak.shellylauncher/.ShellyUpdateAccessibilityService
```

Use the outcome to target a regression test and fix, then confirm both an
accepted click and actual disappearance. Preserve HOME, Shelly credentials and
all enabled services. No installation, settings change, reboot, commit or push
was performed during this recurrence investigation. The diagnostic artifact
is prepared for review; it has not been installed.

## Subsequent approved diagnostic run — 2026-11-13

After explicit approval, the refreshed logging APK (version 1.1.1/code 3,
SHA-256 `aaea1e793e5b99abf88354bb1718e1cf8bd63c557e6a2eba3c139164799094ca`)
was installed with `adb install -r` (Success). The existing popup service stayed
enabled; no service-list/HOME setting was changed. No version 1.2 shortcut was
installed or enabled. The bound helper was PID 8811 with flags 16.

No UIAutomator was used. Shelly was brought forward, then restarted once without
clearing data. The logs confirmed relevant events, real inspections, and one
`child_unavailable` retry. Early loading trees contained 3–4 nodes; larger trees
initially had no required popup literals. At device-log time 10:12:16, the helper
reported a strict snapshot match: title=1, message=1, dismiss=1, store=1. It then
reported `ACTION_CLICK returned=true attempt=1` and `click_accepted MATCH`.
The service dump independently confirmed click_attempts=1, accepted_clicks=1.

Later inspections at 10:12:42 and 10:13:02 found no popup literals. The final
snapshot had 59 nodes, title/message/buttons all zero. This establishes actual
matching and an accepted click in this run, followed by an accessible hierarchy
without the popup. Visual confirmation was separately requested from the user.

The roughly minute-long startup inspection sequence before detection does not
prove the modal was visibly present throughout. The cause of intermittent or
delayed behavior is still unproven. This run matched with existing flags, so it
does not justify changing flags or weakening matching on speculation.

The filtered helper-only capture is in ignored
`build/shelly-update-helper-capture.log`; the live reader was stopped afterward.
No screen text, account/credential data or raw unrelated logs were saved. Final
HOME resolution remained com.ak.shellylauncher/.MainActivity and the enabled
service list still contained only the approved popup helper. No reboot,
firmware/network change, commit or push was performed.
