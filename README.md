# Honor Launcher Unlock

LSPosed module for Honor devices that removes the vendor restriction forcing `com.hihonor.android.launcher` as the only allowed default HOME app.

## What it does

- Bypasses Honor HOME AntiMal checks in `system_server`
- Redirects stable default-HOME settings and role-request entries to the module picker
- Sets the selected launcher through Android's `cmd role add-role-holder` and
  `cmd package set-home-activity` paths

## Current targets

- `android`
- `com.android.settings`
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
- Also enable the module for `com.android.settings` when LSPosed does not apply the suggested scope automatically
- Third-party launcher buttons that directly open HOME settings may show Android's resolver first unless that launcher process is also in scope
- After updating the module, reboot or force-stop the target process where practical so LSPosed reloads the hook
- Tested against a system where Nova Launcher is installed as a third-party HOME candidate
- The module avoids hooking Honor PermissionController obfuscated methods such as `r2.b.c` or `r2.b.d`
