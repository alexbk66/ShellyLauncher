# NSPanel reboot validation — 2026-11-13

The user explicitly authorized one reboot and monitoring for the update popup.
No APK was installed or rebuilt for this test. The installed popup-only launcher
remained version 1.1.1; the pending 1.2 shortcut feature was not enabled.

Verified:

- Boot ID changed from `65ca0cdb-2f85-4134-88f0-e65550cf0786` to
  `788e1c93-ecb7-4d33-b172-5344f8346e57`; `sys.boot_completed` returned 1.
- ADB TCP reconnected after boot, without root/firmware/settings changes.
- HOME still resolved to `com.ak.shellylauncher/.MainActivity`.
- Shelly `MainActivity_Default` automatically reached and remained foreground.
  No manual Shelly launch, force-stop or UI interaction was injected.
- The existing popup-helper enabled-service entry persisted; Android bound the
  helper in the new boot (PID 1089, flags 16).
- Initial root-unavailable inspections exercised the five bounded retries.
  Subsequent Shelly events resumed normal inspections after that retry window.
- At device uptime 188.99 seconds, the helper reported 99 relevant events,
  19 inspections, zero click attempts and zero accepted clicks. Its last snapshot
  had 59 nodes, with zero matching title/message/buttons.
- The user visually confirmed: "I haven't seen the popup" after the reboot.

Only `ShellyUpdateHelper` tagged logs were captured, without UIAutomator, raw
screen data or unrelated application logs. The saved capture is
`build/shelly-update-helper-reboot.log` (git-ignored). Its wall-clock timestamps
jumped during early startup; monitoring duration was measured using /proc/uptime.
The live log reader was stopped after the observation window.

Conclusion: automatic startup, persistent helper enablement and a popup-free
startup were verified over just over three minutes. Because no popup appeared,
this run does not test another automatic dismissal. The earlier accepted click
and user-confirmed disappearance remain separate evidence. Intermittent behavior
is not declared permanently fixed.

No HOME/service-list setting, network or firmware modification, new service,
commit or push occurred. The only reboot was the one explicitly requested.

## Later observation in the same boot

The next capture found a popup after the initial three-minute window: at
10:20:47.763 the same helper detected a strict match, and at 10:20:47.796
ACTION_CLICK returned true (attempt 1). The initial popup-free observation was
therefore time-limited. See `docs/restart-validation.md` for the subsequent,
separate controlled restart and accepted click number 2.
