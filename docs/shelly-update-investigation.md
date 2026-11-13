# Shelly Smart Control update prompt investigation

Investigation date: 2026-11-12 (Australia/Sydney).

## Evidence and mechanism

Read-only ADB investigation at `192.168.68.124:5555` pulled the installed base APK
and density split to ignored `build/shelly-analysis/apks`. No private preferences,
databases, WebView storage, credentials, or user images were copied. ADB was already
root (`id -u` returned `0`); no `adb root` command was needed.

| Artifact | Verified value |
| --- | --- |
| Package | `cloud.shelly.smartcontrol` |
| APK version | `1.56.1/2228e777`, versionCode `260709102` |
| Base APK SHA-256 | `f78b5ff39d0d882f701d59c4d123fc3faa2aca83cdbcd09ccdc74ed1c2eb939f` |
| Density split SHA-256 | `e3604dcdca2096cdd044d6555756b19c14513baf4a59c48285a4042078edfe5a` |
| Original signing certificate SHA-256 | `148dec31d049e976b94504c7feba4a85e8b3e80d6bf9a6a6ba825fe450ff2457` |

The base APK has four DEX files, an `assets/shelly_files.zip` archive (824 entries),
and no packaged `lib/*.so` libraries. Analysis used SDK `dexdump`/`aapt`, Python's
standard library and the original signature verifier. No decompiler or other
application was installed on the panel or workstation.

The native Java/Kotlin side is a WebView host. DEX disassembly confirms:

- `MainActivity.onCreate` sets a `Shelly-1.56.1/2228e777;...;Android-...` user agent.
- `MainActivity.loadFilesFromInternalStorage` loads extracted web files.
- Startup/resume invokes `startWebFilesDownloading[IfOnline]` and
  `MainActivityViewModel.downloadWebFiles`.
- `WebFilesManager` retrieves `/manifest_app.json` and `/index.html`, downloads
  changed assets, and keeps code under `files/shelly_files`.
- Native web development preferences exist (`webDevServerUrl`,
  `webDevServerPersistUrl`, `webDevCachingFilesAllowed`), but they are not a
  per-prompt suppression setting. Redirecting or freezing the entire frontend
  would change substantially more than this prompt and is not recommended.

The runtime index references downloaded bundles different from the APK:

| Bundle | APK copy | Runtime copy |
| --- | --- | --- |
| Application | `index-a728ad3a.js` | `index-04b69511.js` |
| Vendor | `vendor-5ab2152f.js` | `vendor-5aba1a9a.js` |

Only those two runtime **code** files were pulled. Application bundle SHA-256:
`5cecd0b2ab805648b172b457520d4b45b52558f55b8348ad2d45de2bb750613d`.
Vendor SHA-256:
`bc93aee30908fc6e985696e74fa8d381d36f8db3ab18db8d7b79bd0dd95aa5e9`.

The downloaded application bundle contains the exact prompt at character offsets
approximately 7,618,600–7,621,200 (offsets apply only to this bundle):

```text
Title: Update Shelly Smart Control
Message: A newer version of the Shelly Smart Control app is available.
         Update it from the store to continue with the latest version.
Buttons: Not now / Go to Store
```

The relevant minified symbols are `vtn`, `ktn`, `c$s`, `Dze`, `d$s`, and `r$s`.
Readable equivalent of the observed policy:

```text
android: enabled = true; minimum native app version = 1.58.26
ios:     enabled = false
if native-app context and recognized OS and valid app version < minimum:
    show nativeAppUpdateState, priority 110
    remember shown = true in a module-level JavaScript boolean
Not now / dialog close / handled Android back -> emit close event
Go to Store -> open the configured store URL
```

The check reads native-app context, OS, and native app version from frontend
stores. Its call site uses the bundled default `vtn` object. There is no network
request inside this check, no Android API-level compatibility check, no saved
"ignore" preference, and no localStorage/shared-preference write in this prompt's
close handler. `Dze` begins `false` on each new JavaScript module/page lifetime.
The same module is registered in the startup modal queue. This explains recurrence
after frontend reload or a cold start despite choosing Not now previously.

This is distinct from device firmware updates, rating requests, What's New,
browser smart-app banners, and feature-specific "App update required" warnings.
Changing those features' dismissal settings would not suppress this modal.

## Configuration conclusion

No supported persistent suppression setting was found in the traced popup path.
This conclusion is based on the actual downloaded code, not only a keyword search
of the older bundled assets. It does not prove Shelly has no unpublished
server-side or future configuration option. No settings or private app data were
edited to test speculative preference names.

Editing runtime JavaScript, overriding the user agent/version, redirecting web
development URLs, freezing downloads, network filtering, and APK patching were
not used. These would alter Shelly's behavior or update delivery beyond a normal
dialog dismissal. The original APK and certificate remain intact.

## Optional launcher approach

An Android `AccessibilityService` can inspect accessible UI nodes and invoke the
button's `ACTION_CLICK`. A normal launcher Activity cannot control another app's
window just because it is HOME. An accessibility service needs explicit user
enablement; root, overlays, coordinate taps, and network changes are unnecessary.

References:
- https://developer.android.com/guide/topics/ui/accessibility/service
- https://developer.android.com/reference/android/view/accessibility/AccessibilityNodeInfo

The proposed helper is scoped to Shelly and requires all of the following:

1. Active window package exactly `cloud.shelly.smartcontrol`.
2. Exact English title and either the full fallback message or the verified
   English translation from the downloaded code.
3. An enabled, visible, clickable Android Button labelled `Go to Store`.
4. Exactly one enabled, visible, clickable Android Button labelled `Not now`.
5. Unique title and message nodes. Both buttons must be descendants of the
   smallest container shared by those nodes, below the WebView root. The generic
   modal source has separate header/content/footer under a shared container;
   matching only at window/WebView level is rejected. Conflicting nonempty text
   and content descriptions are also rejected.
6. A complete bounded snapshot; ambiguity, missing nodes or changed copy means
   no action. No generic Back, gestures, coordinate taps or generic Cancel clicks.

Only Not now is clicked. Text is never logged, persisted or transmitted. A short
cooldown limits duplicate events, and there is no polling loop. Enablement remains
a separate Android setting requiring approval. No setting is changed by building.

## Verification status and limits

Verified locally/on-device: package metadata, hashes, original certificate,
native DEX behavior, differing runtime code, exact runtime dialog and policy code.
The app was briefly stopped/restarted without clearing its data to reproduce
startup. Initial streamed UI reads did not capture the update dialog. After the
user confirmed the exact prompt, a later read-only capture did identify it.
No click was injected, and the popup was left open.

Verified live: the title and message are `android.widget.TextView` nodes;
Not now and Go to Store are enabled, clickable `android.widget.Button` nodes.
All four are in one nine-node `android.view.View` prompt container below the
WebView. The complete hierarchy had 73 nodes and maximum depth 21, within the
helper's configured limits. UIAutomator exposes visible nodes; actual service
visibility/focus behavior still needs validation after approved enablement.

The displayed English message differs from the component's fallback text:

```text
A new version of Shelly Smart Control is available, including important security
enhancements, performance optimizations, and stability improvements. Update the
app from your store to ensure the best and most secure experience.
```

This exact public text was independently located at `lWo` / `text_native_app_update`
in runtime English code `en-31f2a6aa.js` (SHA-256
`cb49be43425c2ca3ee24939de7f8084ef2ccb7be68585c68c20b1439b57c5a6d`).
The other referenced English module was absent and was not copied. The matcher
now accepts these two exact known English messages, not arbitrary update text.

The raw hierarchy stayed in process memory. The saved regression fixture at
`tests/fixtures/shelly-update-prompt-en.tsv` contains only parent indices, classes,
package names, boolean flags, and the four public prompt literals. Every other
nonempty text/content-description field is replaced with the fixed marker
`<redacted>`. No raw UI dump or private screen text was saved. The original
candidate failed the captured fixture (expected Not now node 21, returned -1);
after adding the verified English message, the same fixture passed.

Not yet verified: whether the installed service's ACTION_CLICK dismisses the
prompt on this panel, or behavior across later frontend changes/locales. The
helper is a candidate with verified live hierarchy matching, not yet a proven
on-panel automated dismissal. It does nothing if matching requirements are unmet.

No APK installation, accessibility enablement, HOME change, firmware change,
network block, reboot, commit or push is part of this investigation/build.

## Local candidate build

Launcher version `1.1` / versionCode `2` includes the optional service. The normal
HOME launch guard and button remain in place. `build.ps1` now packages `res` and
runs both policy test programs. The build passed 7 automatic-launch cases and
29 strict prompt cases (including the sanitized live hierarchy) plus 14 bounded
unavailable-hierarchy retry cases, compiled against API 27, and passed APK v2/v3 signature
and zip alignment verification. Packaged XML confirms the Shelly-only event
filter, window-content capability, and `BIND_ACCESSIBILITY_SERVICE` protection.
The launcher signing certificate matches the earlier launcher debug build.
The APK is `dist/ShellyLauncher-debug.apk`; it has not been installed.
Final candidate APK SHA-256:
`d2706014a7cf67ec0f22f611eef9349198ba075ce5601490ac89c04f6d5de25b`.

Independent code review found event-debounce starvation, window-wide matching
without prompt ancestry, and contradictory label acceptance. The revised service
keeps one pending inspection rather than resetting its delay on every event. The
matcher now requires common prompt ancestry and rejects contradictory labels;
regression cases were observed failing before those matcher fixes. Host tests do
not exercise the Android Handler/node lifecycle or prove real-panel dismissal.

Follow-up user evidence independently confirms both buttons have empty resource
IDs and the prompt recurs on startup, sometimes disappearing temporarily. No
resource IDs, bounds or display-coordinate selectors are used by the helper.
For an initially null, incomplete or stale tree, it now permits five short
delayed retries (300/600/1200/1800/1800 ms), only if scheduled within an eight-second
window. A usable nonmatching tree, foreground app change, or permission failure
ends that sequence. New Shelly accessibility events can begin a new sequence;
there is no permanent polling loop. The service contains no activity-launch
calls and does not modify the HOME guard or startup behavior.

Maintenance access remains a separate, unimplemented proposal in
`docs/maintenance-access-proposal.md`. No maintenance feature is in this APK.

## Subsequent approved installation and activation

The user explicitly approved installing the built launcher and enabling its
popup service. The existing APK with SHA-256
`d2706014a7cf67ec0f22f611eef9349198ba075ce5601490ac89c04f6d5de25b`
was installed using `adb install -r` (Success). Device package metadata confirms
versionName `1.1`, versionCode `2`, min/target SDK 27.

Prior accessibility state was recorded in ignored
`build/accessibility-before-helper.json`. There were no previously enabled
accessibility services. Only
`com.ak.shellylauncher/com.ak.shellylauncher.ShellyUpdateAccessibilityService`
was enabled; Android reports it bound/running. Final HOME resolution still points
to `com.ak.shellylauncher/.MainActivity`; no HOME-setting command was issued.

Observed result: the previously visible popup was absent after enablement
(sanitized UI count changed from 73 with the popup to 63 without it), while
Shelly remained the resumed activity. The helper remained enabled/running at
the end, and the captured crash buffer contained no launcher process crash.

For a controlled comparison the helper was briefly paused and Shelly restarted
without clearing data. The first UI read returned a transient null root; the
second startup check captured a still-loading nine-node hierarchy. The approved
helper state was restored in finally blocks after each attempt. A subsequent
settled read again showed 63 nodes and no update-prompt literals. These checks
do not independently prove a second popup was shown and clicked, since the
popup can disappear temporarily. No raw private UI text or crash logs were saved.

The initial before/after enablement behavior supports successful dismissal;
ACTION_CLICK's return value is not instrumented, and repeated cold-start behavior
remains a useful further validation. There was no reboot, firmware/network
change, Shelly APK modification, credential/data clearing, commit or push.
