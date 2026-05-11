# Honor Launcher Unlock

LSPosed module for Honor devices that removes the vendor restriction forcing `com.hihonor.android.launcher` as the only allowed default HOME app.

## What it does

- Bypasses Honor HOME AntiMal checks in `system_server`
- Allows third-party launchers to appear in the default launcher settings flow
- Blocks PermissionController from forcing HOME back to Honor Launcher during selection

## Current targets

- `android`
- `com.android.permissioncontroller`

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
- Enable the module for `android`, `system`, and `com.android.permissioncontroller`
- After updating the module, reboot or force-stop the target process where practical so LSPosed reloads the hook
- Tested against a system where Nova Launcher is installed as a third-party HOME candidate
