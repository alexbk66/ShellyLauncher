# Shelly Launcher

Minimal native Java Android 8.1 (API 27) HOME launcher for a Sonoff NSPanel Pro 120.
Opens `cloud.shelly.smartcontrol/cloud.shelly.smartcontrol.MainActivity_Default`.
No Gradle, external libraries, network permissions or boot receiver.

## Build

```powershell
.\build.ps1
# Optional overrides:
.\build.ps1 -SdkPath C:\My\Android\Sdk -JdkPath 'C:\path\to\jdk-17'
```

Requires JDK 17, Android platform 27 and Build Tools 35.0.0. JAVA_HOME is resolved
from the process, then persisted user/machine settings. The script runs the JVM
tests, compiles against API27, dexes, packages, aligns, signs and verifies
`dist/ShellyLauncher-debug.apk`. Failures stop the build; the output APK is
replaced only after verification. Each build uses a fresh directory under `build`.

The project-local `.local/debug.keystore` is reused (debug password `android`).
Keep that key to sign future updates with the same identity. Build outputs,
device logs, APKs and signing keys are excluded from Git. Building does not
install anything, enable services or change the device.

Use conventional `View.OnClickListener` implementations: this SDK-only API27
build lacks the lambda factory stub required by javac's lambda compilation.

## Launcher and Settings

The manifest registers MAIN, HOME and DEFAULT. The first activity creation per
boot records an attempt in private preferences before opening Shelly. Returning
HOME, process recreation, or a Shelly failure/exit does not automatically retry.
There is no onResume autolaunch. Reboot allows another automatic attempt.

If the firmware hides the boot counter, the first attempt still works, but later
starts require **Launch Shelly** until the counter is available. A failed guard
write suppresses automatic launch. Missing, disabled or inaccessible Shelly
activities show an error on the fallback screen.

The launcher provides **Launch Shelly** and **Android Settings**. Opening the
launcher deliberately through the floating control suppresses/consumes that
boot's automatic attempt, including on process recreation. Settings uses Android's
standard Settings intent. Shelly stays running in the background.

## Optional update prompt helper

**Shelly update prompt helper** is an independent, opt-in accessibility service.
It presses **Not now** only for the recognized English **Update Shelly Smart
Control** popup. It requires the exact title, a verified body message, both
enabled buttons, unique matches and a shared popup container below the WebView
root. Conflicting labels, unrelated prompts and incomplete matches are rejected.
It never modifies Shelly, updates it, clears its data or changes cloud access.

Inspection is package-scoped and bounded to 600 nodes/24 levels. Unavailable or
stale trees allow up to five delayed retries within eight seconds; usable
nonmatching trees stop the sequence. A five-second action cooldown also applies
to rejected clicks. Actions never fall back to screen coordinates or Back.

Logcat tag **ShellyUpdateHelper** reports lifecycle, throttled event summaries,
fixed matching/retry reasons, detection and ACTION_CLICK results. Diagnostics
contain counters and fixed codes, never screen text, credentials or account
information. Screen contents remain in memory and are not saved or transmitted.
See [logging](docs/update-helper-logging.md) and
[the investigation](docs/shelly-update-investigation.md).

## Optional floating navigation

**Shelly floating navigation** is a separate opt-in accessibility service. Over
Shelly, tap **☰** to expand **Back · Shelly Launcher · Recent apps**. Drag the
handle to move it; its position is saved as screen fractions. Controls collapse
after five seconds and recreate on display configuration changes.

The overlay uses 48dp controls and consumes touches only within its window.
It observes window metadata without screen-content retrieval, root or a
draw-over-apps permission. Back and Recents use Android accessibility global
actions; Launcher uses an explicit intent that prevents autolaunch bounce.

Keep the system navbar available: this test control hides outside Shelly and is
not a full system-navigation replacement. It waits for a Shelly window event
after service connection. Transient other windows can hide it until another
Shelly event; intermittent removal is not fully explained. A bounded in-memory
transition history is available through the service's dumpsys diagnostics.
See [floating-navigation validation](docs/floating-navigation-plan.md).

Enable/disable either service independently in Android Accessibility settings.
Installation and enablement are separate from building. No reboot button is
implemented, and the system navigation bar is not changed by the app.

## Device validation

The panel was running version **1.3.2/code 8** at the latest device check. The
launcher is default HOME and both optional services are enabled. Verified cases
include repeated strict popup matches/clicks, startup after an approved reboot,
Launcher/Settings navigation, floating-control expansion/auto-collapse, dragging
and opening Recents. Android accepted the floating Back action; every possible
Shelly page's Back behavior has not been tested. Rotation/scaling and popup
dismissal with the new floating control still need further observation.

UIAutomator normally suppresses accessibility services. Do not use it to test
overlay persistence or popup dismissal. Research artifacts and pulled Shelly
APKs were moved outside the project; none belong in Git.

See [reboot validation](docs/reboot-validation.md),
[restart validation](docs/restart-validation.md), and
[launcher-control history](docs/launcher-controls-plan.md). Historical plans and
validation hashes describe their individual builds; this README describes the
current behavior.
