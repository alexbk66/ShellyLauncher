# Shelly Launcher

Minimal native Java Android 8.1 (API 27) HOME launcher for a Sonoff NSPanel Pro 120.
Opens `cloud.shelly.smartcontrol/cloud.shelly.smartcontrol.MainActivity_Default`.
No Gradle, external libraries, network permissions, or boot receiver.

## Build

```powershell
.\build.ps1
# Optional overrides:
.\build.ps1 -SdkPath C:\My\Android\Sdk -JdkPath 'C:\path\to\jdk-17'
```

Requires JDK 17, Android platform 27 and build tools 35.0.0. The script uses
JAVA_HOME (process, then persisted user/machine settings). It runs seven JVM
launch-policy cases and compiles, dexes, packages, aligns, signs, and verifies
`dist/ShellyLauncher-debug.apk`. Each build has a fresh directory under `build`.
Failures stop the build; the dist APK is replaced only after verification.
The project-local `.local/debug.keystore` is reused. Its password is `android`;
this is a debug build/key, not a production release. Keep the key to sign future
updates with the same identity. Generated files and the key are git-ignored.

The initial build exposed a JDK 17/API 27 lambda compiler error (`Unable to find
method metafactory`): the platform jar omits the compiler's lambda factory stub.
The button now uses a conventional `View.OnClickListener`, avoiding that extra
compiler dependency. Key generation can emit a nonfatal warning about JKS being
a proprietary format; APK signature verification is still required and runs
after signing.

## Launch behavior

Android can offer this app as HOME because the manifest declares MAIN, HOME,
and DEFAULT. The first activity creation per boot records an attempt in private
preferences before opening Shelly. Returning HOME, activity/process recreation,
and Shelly failure or exit do not automatically retry. Reboot permits another
automatic attempt. If firmware hides the boot counter, the first attempt still
works, but later starts require the button until the counter is available or
app data is cleared. A failed preference write suppresses the automatic attempt.

The fallback screen always has **Launch Shelly** for manual retries. Missing,
disabled, or inaccessible target activities show an error. The supplied activity
is launched explicitly; no alternative package or activity is guessed. The app
cannot detect a later crash inside Shelly, so it stays available underneath it.

## Deferred device checks

No install or HOME selection is performed by the build script. Once device work
is separately authorized, verify on the NSPanel: first HOME entry opens Shelly;
returning HOME and exiting/crashing Shelly do not loop; the button retries;
missing/disabled Shelly shows fallback; reboot allows one new attempt; and
launcher process recreation does not reset the guard. These runtime cases have
not been validated by local compilation or the JVM policy tests.

## Optional update prompt helper

An opt-in accessibility service dismisses only the exact English Shelly update
popup using strict title/body/button/container checks. Bounded startup retries
handle unavailable trees; diagnostics use throttled fixed reasons and counters,
never screen content or credentials. Enablement is separate from building.
See [the investigation](docs/shelly-update-investigation.md),
[logging](docs/update-helper-logging.md) and
[device validation](docs/restart-validation.md).
