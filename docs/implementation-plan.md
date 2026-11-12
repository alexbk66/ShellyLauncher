# Shelly launcher implementation plan

Goal: Build a minimal API 27 HOME launcher that opens Shelly Smart Control.

Use the existing HOME/DEFAULT manifest and a native Java Activity with a status
message and Launch Shelly button. Attempt the supplied explicit Shelly component
once per device boot, recording the attempt synchronously before starting it.
Persist the guard so process recreation, returning HOME, and target crashes do
not trigger repeated automatic launches. Manual retries remain available.

1. Test the boot-based automatic launch policy on the JVM, including unknown boot
   counts, repeated starts, and reboot.
2. Implement MainActivity with the persistent guard and unavailable/blocked app
   handling; retain the existing HOME registration.
3. Add a PowerShell build using JDK 17, platform 27, and build tools 35.0.0:
   javac, D8, aapt, zipalign, keytool, and apksigner. Keep the debug key local.
4. Run policy tests, build the APK, verify signatures/alignment/manifest/DEX,
   and document build and future on-device checks.

No device connection, installation, or HOME configuration changes are authorized.
Runtime behavior must be checked on the panel later; local build validation does
not establish compatibility with the panel firmware or installed Shelly version.
