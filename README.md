# Honor Launcher Unlock

LSPosed module for Honor devices that removes the vendor restriction forcing `com.hihonor.android.launcher` as the only allowed default HOME app.

## What it does

- Bypasses Honor HOME AntiMal checks in `system_server`
- Keeps real third-party HOME apps visible in the system default-launcher list
- Leaves the system default-launcher UI in place so users can switch both to
  third-party launchers and back to Honor Launcher

## How it works

The module hooks the restriction at the semantic boundary first, then falls
back to device-specific compatibility paths only when needed:

- In `system_server`, it hooks
  `com.android.server.pm.PreferredActivityHelper#isAlreadyDefaultHomeActivity`
  so Honor's package-manager HOME gate does not reject third-party launchers.
- In PermissionController, it supports the normal Role model APIs
  `getQualifyingPackagesAsUser` and `isPackageQualifiedAsUser`, which keeps
  real HOME apps in the system default-app UI.
- On current Honor Android 16 builds where PermissionController's Role model is
  obfuscated as `c2.p`, it also hooks the equivalent methods
  `i(UserHandle, Context)` and `y(String, UserHandle, Context)`.
- For Honor's launcher AntiMal filter, the primary hook is the stable policy
  boundary:
  `com.hihonor.android.securitydiagnose.HwSecurityDiagnoseManager#getAntimalProtectionPolicy`.
  The module only overrides the result when the checked package is a real HOME
  activity candidate. The obfuscated UI helper `r2.b#d(String, int)` is kept
  only as a fallback if the stable Honor API is unavailable.

## Current targets

- `android`
- `com.android.permissioncontroller`
- `com.google.android.permissioncontroller`

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
- Enable the module for `android`, `system`, `com.android.permissioncontroller`,
  and `com.google.android.permissioncontroller` if that package exists on the
  device
- After updating the module, reboot or force-stop the target process where practical so LSPosed reloads the hook
- Tested against a system where Nova Launcher is installed as a third-party HOME candidate

Useful LSPosed log lines when verifying:

```text
HonorLauncherUnlock: hooked PreferredActivityHelper.isAlreadyDefaultHomeActivity
HonorLauncherUnlock: hooked HwSecurityDiagnoseManager.getAntimalProtectionPolicy(int, Bundle) for HOME
HonorLauncherUnlock: allowing Honor HOME AntiMal policy for com.teslacoilsw.launcher type=1 user=0
```

On Honor Android 16 builds with obfuscated PermissionController Role classes,
these compatibility lines are also expected:

```text
HonorLauncherUnlock: discovered Role model class c2.p
HonorLauncherUnlock: hooked c2.p.i(UserHandle, Context) for HOME
HonorLauncherUnlock: hooked c2.p.y(String, UserHandle, Context) for HOME
```
