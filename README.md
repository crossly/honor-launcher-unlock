# Honor Launcher Unlock

LSPosed module for Honor devices that removes the vendor restriction forcing `com.hihonor.android.launcher` as the only allowed default HOME app.

## What it does

- Bypasses Honor HOME AntiMal checks in `system_server`
- Leaves the system default-launcher UI in place so users can switch both to
  third-party launchers and back to Honor Launcher

## Current targets

- `android`

## Build

Requires:

- JDK 17
- Android SDK build-tools 36

Build release APK:

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew :app:assembleRelease
```

Build debug APK:

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew :app:assembleDebug
```

Run unit tests:

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew :app:testDebugUnitTest
```

## Notes

- Intended for rooted Honor devices with LSPosed
- Enable the module for `android` / `system`
- After updating the module, reboot or force-stop the target process where practical so LSPosed reloads the hook
- Tested against a system where Nova Launcher is installed as a third-party HOME candidate
