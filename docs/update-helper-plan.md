# Optional update helper implementation plan

Goal: Offer ordinary Not now dismissal for the identified Shelly WebView modal,
preserving the original Shelly APK, signing certificate and application data.

Spec: `docs/shelly-update-investigation.md`; user restrictions govern this work.
Implementation happened in the requested source directory. The initial plan
deferred commits; the user subsequently authorized reviewed cleanup and commits.

1. Add a platform-independent strict prompt matcher and failing JVM cases for
   exact dialog, wrong package, generic update/firmware dialogs, duplicate
   buttons, hidden/disabled controls, changed/localized text and incomplete reads.
2. Add an opt-in AccessibilityService scoped to `cloud.shelly.smartcontrol`.
   It reads bounded snapshots, clicks only the selected Not now Button, uses an
   event debounce/cooldown, recycles nodes and never logs window content.
3. Add manifest/service resources and extend the SDK-only build for resources
   and matcher tests. Increment launcher versionCode/versionName for an update.
4. Run all host tests, compile against API 27, build/sign/verify the APK, inspect
   the packaged service configuration, and request a read-only code review.
5. Document that live dialog matching/clicks require later device validation.
   Leave installation and service enablement pending explicit approval.
6. Follow-up: add a bounded, tested unavailable-tree retry schedule so a missing
   initial hierarchy does not depend exclusively on another accessibility event.
   Keep this popup-only change separate from the maintenance-access proposal.

Configuration suppression would be preferable but the observed path has no
persistent setting. Accessibility is selected over root automation because it
uses Android's supported UI action with strict identity checks. English-only,
exact matching sacrifices coverage in favor of avoiding unintended clicks.
