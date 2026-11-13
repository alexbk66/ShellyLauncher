# Delayed popup / Shelly restart validation — 2026-11-13

The user requested a Shelly restart because the popup can appear randomly and
after a delay. Installed software remained popup-only diagnostic 1.1.1. No APK,
settings, HOME, enabled-service or network change was made. No UIAutomator was used.

Before our restart, helper PID 1089 already reported one accepted click. The same
boot's historical logs showed a delayed popup match and ACTION_CLICK=true at
10:20:47, after the earlier reboot-monitoring window had ended. This was treated
as a baseline, not a result of the new restart.

Shelly alone was force-stopped and launched once, preserving its data. Uptime
immediately afterward was 285.17 seconds. After initial loading and nonmatching
hierarchies, a distinct new popup arrived roughly one minute later:

```text
10:22:12.615 Relevant event=WINDOW_CONTENT_CHANGED
10:22:12.816 Inspection attempt=36 retry=0
10:22:13.058 Popup detected: strict snapshot match found
10:22:13.098 ACTION_CLICK returned=true attempt=2
10:22:13.098 click_accepted MATCH nodes=69 title=1 message=1 dismiss=1 store=1
```

The dump independently confirmed click_attempts=2 and accepted_clicks=2. At
10:23:18.172, a later inspection found 59 nodes and zero matching popup literals.
Shelly remained resumed. HOME and the existing enabled-service list were preserved.

The saved helper-only capture is `build/shelly-update-helper-restart.log`
(git-ignored). It includes historical context, so timestamps and the baseline
count distinguish the new result. The live reader was stopped after collection.

This test confirms a delayed accessible popup was matched, its click accepted,
and later accessible state was popup-free. The accepted click followed the
triggering recorded event by about 0.48 seconds. Exact visible-frame timing and
every intermittent case remain unproven. No source/safety-check change was
needed or made; no commit or push occurred.
