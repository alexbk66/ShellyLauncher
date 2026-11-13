# Popup helper Logcat diagnostics

All Android log entries use tag `ShellyUpdateHelper`.

- INFO: service connected, unbound/destroyed, strict popup snapshot detection,
  and the boolean returned by each actual ACTION_CLICK request.
- DEBUG: relevant Shelly window-state/content events, cumulative event counts,
  inspection attempts, outcomes, retry scheduling/stopping and interruption.
- Failed matching reports a fixed `UpdatePromptPolicy.Reason` from the actual
  matcher: for example TITLE_NOT_UNIQUE, MESSAGE_NOT_UNIQUE,
  DISMISS_BUTTON_NOT_UNIQUE, CONTAINER_IS_WEBVIEW, NO_WEBVIEW_ANCESTOR,
  DISMISS_OUTSIDE_CONTAINER or STORE_OUTSIDE_CONTAINER.

NOT_UNIQUE means the required count is not exactly one (zero is also a failure).
The counters-only snapshot summary uses the same strict label/button predicates.
The dump command also exposes `last_match_reason` for the latest inspection.

Frequent event/inspection/outcome/retry categories are each limited to one entry
per five seconds. Each fixed matching-failure reason has its own five-second
limit, so a newly encountered failure is logged immediately without alternating
failures bypassing their individual limits. Detection/interruption are also
throttled. Lifecycle logs are infrequent; actual click results are bounded by the
existing five-second action cooldown. Logging throttles do not schedule,
suppress or change inspections, retries, matching or clicks.

No event/node toString(), original text, content descriptions, foreign package
names, credentials, account details, exception messages or stack traces are
logged. Only fixed literals/enums and numeric/boolean diagnostics are emitted.
The throttle stores timestamps in fixed-size arrays, not UI-derived keys.

After a separately approved installation, inspect logs with:

```powershell
& C:\platform-tools\adb.exe -s 192.168.68.124:5555 logcat -v time -s ShellyUpdateHelper:D '*:S'
```

`Popup detected: strict snapshot match found` precedes the refreshed-button and
focus guards. `ACTION_CLICK returned=true` means Android accepted the action;
it does not independently prove the modal disappeared. The initial logging work
was local only; later approved device tests captured matching/click results and
subsequent popup-free inspections. See [the diagnostic run](popup-recurrence-diagnostics.md)
and [restart validation](restart-validation.md).

Local validation includes all previous matcher/guard/retry/visibility/privacy
cases, nine exact matching-reason cases and fourteen throttling cases. The policy
refactor only exposes existing rejection conditions; it does not relax them.
Both the main project APK and a separate popup-only diagnostic APK were built
during investigation. The normal build script produces only the main APK.
