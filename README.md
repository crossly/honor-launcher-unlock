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
gradle :app:assembleRelease
```

## Notes

- Intended for rooted Honor devices with LSPosed
- Tested against a system where Nova Launcher is installed as a third-party HOME candidate
