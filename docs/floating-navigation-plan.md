# Floating navigation test build

User authorizes installing launcher test builds. Keep the existing bottom navbar
and Android settings unchanged. No reboot, commits or pushes.

Use the existing opt-in shortcut accessibility service, without new permissions
or content retrieval. Replace the text shortcut with a compact navigation icon.
Tap expands Back / Launcher / Recents; drag the handle to reposition. Save the
position as screen fractions and clamp after expansion/display changes. Collapse
after navigation or five seconds. Keep current Shelly-only visibility for this
first test, so the bottom navbar is still needed outside Shelly.

Approaches: changing Sonoff's system navbar needs firmware-specific investigation;
an accessibility overlay reuses the installed service and is reversible. Proceed
with the overlay discussed with the user. Launcher uses the existing explicit
show-launcher intent to avoid autolaunch bounce; Back/Recents use Android global
accessibility actions. Popup helper remains separate and unchanged.

Implementation plan (inline execution, no Git index changes):
- Add regression tests for composite overlay self-events; confirm failure.
- Implement composite icon control, bounded drag positioning, persistence,
  auto-collapse and lifecycle cleanup. Version 1.3/code 6.
- Run host tests/build/signature/alignment verification; review changed source.
- Install approved test APK, verify overlay/window transitions and navigation
  counters without UIAutomator. Keep visual/drag verification explicit.

Review focus: self-events must not hide overlay; dragging must not invoke a
navigation action; canceled touches must not click; expanded controls must remain
on-screen; callbacks must be canceled when service/window goes away.

## Validation, 2026-11-13

Installed final test version 1.3.2/code 8. All 92 host cases pass; API27
compilation, APK signature and alignment checks pass. SHA-256:
`264c42130490159e6a094ecb84b8612a806497de1a15221baa5e95052d78c8bf`.

The composite overlay uses an explicit accessibility class identity
`com.ak.shellylauncher.NavigationOverlay`, matched only within our own package
while a shortcut already exists. No broader foreign-window matching was added.
The service retains eight metadata transitions in memory for dumpsys: sequence,
own class only (other apps reduced to Shelly/other), window IDs, and visibility
decisions. No event text, UI contents, credentials or foreign classes are retained.

Read-only review identified position drift on collapse and multi-pointer
ambiguity. Both were addressed: retain a collapsed anchor independent of expanded
window clamping; cancel the touch sequence after a second pointer. Review found
no further blocker. These touch edge cases still need physical user validation.

Verified through actual overlay touches using current WindowManager bounds:
- Handle expands to four controls; five-second auto-collapse restores its anchor.
- Launcher action brings MainActivity forward and removes the overlay.
- Drag changes position without expansion or navigation; own preferences persist
  normalized coordinates. The first automated checks sampled asynchronous layout
  too early; a measured swipe and subsequent checks with rendering wait succeeded.
- Recents action opens Android SystemUI Recents; returning to Shelly restores
  the overlay. Back action is invoked and Android reports it accepted; this does
  not establish every possible Shelly page's Back behavior.
- Final collapsed handle is visible near the header gap (current frame x384/y29,
  108px square at density override360). User can drag it elsewhere.
- Popup helper remains bound and running. No new popup was matched during these
  checks, so simultaneous popup dismissal was not revalidated.

Two initial composite-control runs removed the overlay before history was
available. The final build's captured self-event keeps it visible, and the
navigation checks pass; the earlier intermittent removal cause remains
unconfirmed. The metadata ring is retained for recurrence investigation.

The bottom navbar, HOME setting and other Android settings were left unchanged.
No reboot, Git staging, commit or push. This remains a Shelly-only test control,
not yet a complete replacement for navigation in Settings or other apps.
